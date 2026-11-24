package com.easetrading.api.portfolio;

import com.easetrading.api.broker.BrokerAdapter;
import com.easetrading.api.broker.BrokerModels.BrokerHolding;
import com.easetrading.api.broker.BrokerModels.DayOhlc;
import com.easetrading.api.common.ApiException;
import com.easetrading.api.instrument.Instrument;
import com.easetrading.api.instrument.InstrumentResolver;
import com.easetrading.api.marketdata.MarketSubscription;
import com.easetrading.api.marketdata.QuoteCache;
import com.easetrading.api.position.PortfolioService;
import com.easetrading.api.position.PortfolioService.RiskView;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.*;

/**
 * "My Portfolio" — your REAL holdings from the broker (Groww), or demo holdings in
 * mock mode, joined with live prices:
 *
 *   broker holdings ──► match each to our stock list ──► live price (LTP) + previous close
 *                   ──► invested, current value, P&L, today's change, allocation, risk
 *
 * Read-only. Holdings are also added to the live price stream so the dashboard updates.
 * (Paper-trading positions live separately — see PortfolioService / GET /api/portfolio/paper.)
 */
@Service
public class BrokerPortfolioService {

    private final BrokerAdapter broker;
    private final InstrumentResolver resolver;
    private final QuoteCache quotes;
    private final MarketSubscription subscription;

    public BrokerPortfolioService(BrokerAdapter broker, InstrumentResolver resolver,
                                  QuoteCache quotes, MarketSubscription subscription) {
        this.broker = broker;
        this.resolver = resolver;
        this.quotes = quotes;
        this.subscription = subscription;
    }

    /**
     * One holding. priceAvailable=false means the broker gave us no price (e.g. Groww's
     * free plan blocks market data): ltp/pnl/day fields are then null, and marketValue
     * is shown AT COST so allocation still adds up — never presented as a real P&L (B24).
     */
    public record HoldingView(String token, String symbol, String name, int qty, double avgPrice,
                              boolean priceAvailable, Double ltp, Double prevClose,
                              double invested, double marketValue,
                              Double pnl, Double pnlPct, Double dayChange, Double dayChangePct,
                              double allocationPct) {}

    /**
     * Totals. P&L figures cover only the holdings that have a live price
     * (pricedHoldings of holdings); when pricedHoldings is 0 the UI shows "—".
     */
    public record Summary(int holdings, int pricedHoldings, double invested, double marketValue,
                          double totalPnl, double totalPnlPct, double dayPnl, double dayPnlPct) {}

    public record PortfolioView(String source, Instant asOf, Summary summary,
                                List<HoldingView> holdings, RiskView risk) {}

    public PortfolioView build() {
        List<BrokerHolding> raw;
        try {
            raw = broker.holdings();
        } catch (Exception e) {
            throw new ApiException(HttpStatus.BAD_GATEWAY, "Could not load holdings: " + e.getMessage());
        }

        // 1) Match holdings to our instruments (needed for prices, charts, links).
        Map<BrokerHolding, Instrument> matched = new LinkedHashMap<>();
        for (BrokerHolding h : raw) {
            if (h.quantity() <= 0) continue;
            matched.put(h, resolver.resolve(h.tradingSymbol(), h.isin()).orElse(null));
        }
        List<Instrument> known = matched.values().stream().filter(Objects::nonNull).distinct().toList();
        known.forEach(subscription::add);

        // 2) Prices in two batched calls (≤50 stocks each).
        Map<String, Double> ltp = safe(() -> broker.ltp(known), Map.of());
        Map<String, DayOhlc> ohlc = safe(() -> broker.ohlc(known), Map.of());

        // 3) Per-holding prices, then totals.
        record Priced(BrokerHolding h, Instrument inst, Double price, Double prev) {}
        List<Priced> priced = new ArrayList<>();
        double totalInvested = 0, totalMarket = 0;
        double pricedInvested = 0, pricedMarket = 0, totalDay = 0, totalPrevValue = 0;
        int pricedCount = 0;
        for (var e : matched.entrySet()) {
            BrokerHolding h = e.getKey();
            String token = e.getValue() == null ? null : e.getValue().getToken();
            Double price = priceFor(token, ltp);
            DayOhlc o = token == null ? null : ohlc.get(token);
            Double prev = price == null || o == null || o.close() <= 0 ? null : o.close();

            double invested = h.quantity() * h.averagePrice();
            double market = h.quantity() * (price != null ? price : h.averagePrice());
            totalInvested += invested;
            totalMarket += market;
            if (price != null) {
                pricedCount++;
                pricedInvested += invested;
                pricedMarket += market;
            }
            if (prev != null) {
                totalDay += (price - prev) * h.quantity();
                totalPrevValue += prev * h.quantity();
            }
            priced.add(new Priced(h, e.getValue(), price, prev));
        }

        List<HoldingView> holdings = new ArrayList<>();
        for (Priced p : priced) {
            BrokerHolding h = p.h();
            Instrument inst = p.inst();
            boolean has = p.price() != null;
            double invested = h.quantity() * h.averagePrice();
            double market = h.quantity() * (has ? p.price() : h.averagePrice());
            Double pnl = has ? r(market - invested) : null;
            holdings.add(new HoldingView(
                    inst == null ? null : inst.getToken(),
                    inst == null ? h.tradingSymbol() : inst.getSymbol(),
                    inst == null ? h.tradingSymbol() : inst.getName(),
                    h.quantity(), r(h.averagePrice()), has,
                    has ? r(p.price()) : null, p.prev() == null ? null : r(p.prev()),
                    r(invested), r(market),
                    pnl, has && invested > 0 ? r((market - invested) / invested * 100) : null,
                    p.prev() == null ? null : r((p.price() - p.prev()) * h.quantity()),
                    p.prev() == null ? null : r((p.price() - p.prev()) / p.prev() * 100),
                    r(totalMarket > 0 ? market / totalMarket * 100 : 0)));
        }
        holdings.sort(Comparator.comparingDouble(HoldingView::marketValue).reversed());

        double totalPnl = pricedMarket - pricedInvested;
        Summary summary = new Summary(holdings.size(), pricedCount, r(totalInvested), r(totalMarket),
                r(totalPnl), r(pricedInvested > 0 ? totalPnl / pricedInvested * 100 : 0),
                r(totalDay), r(totalPrevValue > 0 ? totalDay / totalPrevValue * 100 : 0));
        RiskView risk = PortfolioService.riskOf(holdings.stream().map(HoldingView::allocationPct).toList());

        return new PortfolioView(broker.source(), Instant.now(), summary, holdings, risk);
    }

    /** Broker LTP, else the last streamed price, else null (= "price unavailable"). */
    private Double priceFor(String token, Map<String, Double> ltp) {
        if (token == null) return null;
        Double p = ltp.get(token);
        if (p != null && p > 0) return p;
        double cached = quotes.latestPrice(token);
        return cached > 0 ? cached : null;
    }

    private static <T> T safe(java.util.function.Supplier<T> s, T fallback) {
        try {
            return s.get();
        } catch (Exception e) {
            System.err.println("[Portfolio] price lookup failed: " + e.getMessage());
            return fallback;
        }
    }

    private static double r(double v) { return Math.round(v * 100.0) / 100.0; }
}
