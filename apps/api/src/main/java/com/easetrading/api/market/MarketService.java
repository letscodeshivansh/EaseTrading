package com.easetrading.api.market;

import com.easetrading.api.broker.BrokerAdapter;
import com.easetrading.api.broker.BrokerModels.DayOhlc;
import com.easetrading.api.common.TtlCache;
import com.easetrading.api.instrument.Indices;
import com.easetrading.api.instrument.Instrument;
import com.easetrading.api.instrument.InstrumentStore;
import com.easetrading.api.marketdata.CandleDto;
import com.easetrading.api.marketdata.CandleService;
import com.easetrading.api.marketdata.MarketHours;
import com.easetrading.api.marketdata.QuoteCache;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;

/**
 * "How is the market doing?" — the same for every user, so it is built once and
 * shared (cached a few seconds):
 *   - the main indices with live value, day change and a 30-day sparkline
 *   - top gainers / losers among Nifty 50 stocks
 *   - market breadth: how many of them are up vs down today
 */
@Service
public class MarketService {

    /** Nifty 50 constituents (approximate — the index is rebalanced twice a year). */
    public static final List<String> NIFTY_50 = List.of(
            "RELIANCE", "HDFCBANK", "ICICIBANK", "INFY", "TCS", "BHARTIARTL", "ITC", "LT", "SBIN", "KOTAKBANK",
            "AXISBANK", "HINDUNILVR", "BAJFINANCE", "M&M", "MARUTI", "SUNPHARMA", "HCLTECH", "NTPC", "TITAN",
            "ULTRACEMCO", "ASIANPAINT", "POWERGRID", "TATASTEEL", "ADANIPORTS", "BAJAJFINSV", "NESTLEIND",
            "JSWSTEEL", "ONGC", "TECHM", "WIPRO", "COALINDIA", "GRASIM", "HINDALCO", "CIPLA", "DRREDDY", "BEL",
            "TRENT", "SBILIFE", "HDFCLIFE", "BRITANNIA", "EICHERMOT", "APOLLOHOSP", "INDUSINDBK", "TATACONSUM",
            "SHRIRAMFIN", "HEROMOTOCO", "BAJAJ-AUTO", "ADANIENT", "JIOFIN", "TATAMOTORS");

    public record IndexCard(String token, String symbol, String name, Double ltp, Double prevClose,
                            Double change, Double changePct, List<Double> spark) {}

    public record Mover(String token, String symbol, String name, double ltp, double changePct) {}

    public record Breadth(int advances, int declines, int unchanged) {}

    public record Overview(boolean marketOpen, Instant asOf, List<IndexCard> indices,
                           List<Mover> gainers, List<Mover> losers, Breadth breadth,
                           int basketSize, String message) {}

    private final BrokerAdapter broker;
    private final InstrumentStore store;
    private final QuoteCache quotes;
    private final CandleService candles;
    private final TtlCache<Overview> cache = new TtlCache<>(Duration.ofSeconds(5));
    private final TtlCache<Map<String, DayOhlc>> ohlcCache = new TtlCache<>(Duration.ofSeconds(60));

    public MarketService(BrokerAdapter broker, InstrumentStore store, QuoteCache quotes, CandleService candles) {
        this.broker = broker;
        this.store = store;
        this.quotes = quotes;
        this.candles = candles;
    }

    /** Index + Nifty 50 instruments that exist in the stock list (used for streaming too). */
    public List<Instrument> trackedInstruments() {
        List<Instrument> out = new ArrayList<>(indices());
        out.addAll(basket());
        return out;
    }

    public Overview overview() {
        return cache.get(this::build);
    }

    private Overview build() {
        List<Instrument> idx = indices();
        List<Instrument> basket = basket();
        List<Instrument> all = new ArrayList<>(idx);
        all.addAll(basket);

        String message = null;
        Map<String, Double> ltp = new HashMap<>();
        Map<String, DayOhlc> ohlc = Map.of();
        try {
            ltp.putAll(livePrices(all));
            ohlc = ohlcCache.get(() -> broker.ohlc(all));
        } catch (Exception e) {
            message = "Live prices unavailable: " + e.getMessage();
        }

        List<IndexCard> cards = new ArrayList<>();
        for (Instrument i : idx) {
            Double price = ltp.get(i.getToken());
            Double prev = prevClose(ohlc, i);
            Double change = price != null && prev != null ? r(price - prev) : null;
            Double pct = price != null && prev != null ? r((price - prev) / prev * 100) : null;
            cards.add(new IndexCard(i.getToken(), i.getSymbol(), i.getName(), price, prev, change, pct, spark(i)));
        }

        List<Mover> movers = new ArrayList<>();
        for (Instrument s : basket) {
            Double price = ltp.get(s.getToken());
            Double prev = prevClose(ohlc, s);
            if (price != null && prev != null) {
                movers.add(new Mover(s.getToken(), s.getSymbol(), s.getName(), r(price), r((price - prev) / prev * 100)));
            }
        }
        int up = (int) movers.stream().filter(m -> m.changePct() > 0).count();
        int down = (int) movers.stream().filter(m -> m.changePct() < 0).count();
        List<Mover> gainers = movers.stream().filter(m -> m.changePct() > 0)
                .sorted(Comparator.comparingDouble(Mover::changePct).reversed()).limit(5).toList();
        List<Mover> losers = movers.stream().filter(m -> m.changePct() < 0)
                .sorted(Comparator.comparingDouble(Mover::changePct)).limit(5).toList();

        return new Overview(MarketHours.isOpen(Instant.now()), Instant.now(), cards, gainers, losers,
                new Breadth(up, down, movers.size() - up - down), basket.size(), message);
    }

    /** Streamed price if we have one, otherwise ask the broker (one batched call). */
    private Map<String, Double> livePrices(List<Instrument> all) {
        Map<String, Double> out = new HashMap<>();
        List<Instrument> missing = new ArrayList<>();
        for (Instrument i : all) {
            double p = quotes.latestPrice(i.getToken());
            if (p > 0) out.put(i.getToken(), p);
            else missing.add(i);
        }
        if (!missing.isEmpty()) out.putAll(broker.ltp(missing));
        return out;
    }

    private static Double prevClose(Map<String, DayOhlc> ohlc, Instrument i) {
        DayOhlc o = ohlc.get(i.getToken());
        return o == null || o.close() <= 0 ? null : o.close();
    }

    /** Last 30 daily closes for the little trend line. */
    private List<Double> spark(Instrument i) {
        try {
            Instant now = Instant.now();
            List<CandleDto> bars = candles.getCandles(i.getToken(), "1d", now.minus(45, ChronoUnit.DAYS), now);
            return bars.stream().skip(Math.max(0, bars.size() - 30)).map(CandleDto::close).toList();
        } catch (Exception e) {
            return List.of();
        }
    }

    private List<Instrument> indices() {
        return Indices.ALL.stream().map(x -> store.get(x.token()).orElse(null)).filter(Objects::nonNull).toList();
    }

    private List<Instrument> basket() {
        return NIFTY_50.stream().map(s -> store.bySymbol(s, "NSE").orElse(null)).filter(Objects::nonNull).toList();
    }

    private static double r(double v) { return Math.round(v * 100.0) / 100.0; }
}
