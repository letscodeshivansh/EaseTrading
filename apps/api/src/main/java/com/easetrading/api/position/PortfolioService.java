package com.easetrading.api.position;

import com.easetrading.api.common.CurrentUserService;
import com.easetrading.api.instrument.Instrument;
import com.easetrading.api.instrument.InstrumentRepository;
import com.easetrading.api.marketdata.QuoteCache;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * Builds the portfolio view: holdings with live P&L, plus a simple Bridgewater-style
 * risk read-out (concentration and diversification). Market values use the latest
 * live price, so P&L is always current — nothing stale is stored.
 */
@Service
public class PortfolioService {

    private final PositionRepository positions;
    private final InstrumentRepository instruments;
    private final QuoteCache quoteCache;
    private final CurrentUserService currentUser;
    private final ObjectMapper json = new ObjectMapper();

    public PortfolioService(PositionRepository positions, InstrumentRepository instruments,
                            QuoteCache quoteCache, CurrentUserService currentUser) {
        this.positions = positions;
        this.instruments = instruments;
        this.quoteCache = quoteCache;
        this.currentUser = currentUser;
    }

    /** One holding row shown to the user. */
    public record Holding(String token, String symbol, int qty, double avgPrice, double ltp,
                          double invested, double marketValue, double pnl, double pnlPct,
                          double allocationPct) {}

    /** Portfolio-level risk read-out. */
    public record RiskView(int holdings, double largestPositionPct, double concentrationIndex,
                           String assessment) {}

    /** The full payload returned to the frontend. */
    public record PortfolioView(List<Holding> holdings, double invested, double marketValue,
                                double totalPnl, double totalPnlPct, RiskView risk) {}

    public PortfolioView build() {
        var userId = currentUser.currentUserId();
        var rows = positions.findByUserId(userId).stream().filter(p -> p.getQty() > 0).toList();

        List<Holding> holdings = new ArrayList<>();
        double totalInvested = 0, totalMarket = 0;

        // First pass: compute market values.
        for (var p : rows) {
            double ltp = latestPrice(p.getToken());
            if (ltp <= 0) ltp = p.getAvgPrice();           // fall back to cost if no live price
            double invested = p.getQty() * p.getAvgPrice();
            double marketValue = p.getQty() * ltp;
            totalInvested += invested;
            totalMarket += marketValue;
        }

        // Second pass: build rows with allocation % (needs the total).
        for (var p : rows) {
            double ltp = latestPrice(p.getToken());
            if (ltp <= 0) ltp = p.getAvgPrice();
            double invested = p.getQty() * p.getAvgPrice();
            double marketValue = p.getQty() * ltp;
            double pnl = marketValue - invested;
            double pnlPct = invested > 0 ? pnl / invested * 100 : 0;
            double allocation = totalMarket > 0 ? marketValue / totalMarket * 100 : 0;

            holdings.add(new Holding(p.getToken(), symbolOf(p.getToken()), p.getQty(),
                    round(p.getAvgPrice()), round(ltp), round(invested), round(marketValue),
                    round(pnl), round(pnlPct), round(allocation)));
        }

        double totalPnl = totalMarket - totalInvested;
        double totalPnlPct = totalInvested > 0 ? totalPnl / totalInvested * 100 : 0;

        return new PortfolioView(holdings, round(totalInvested), round(totalMarket),
                round(totalPnl), round(totalPnlPct), riskView(holdings));
    }

    /** Simple concentration risk: largest holding % and a Herfindahl-style index. */
    private RiskView riskView(List<Holding> holdings) {
        if (holdings.isEmpty()) {
            return new RiskView(0, 0, 0, "No holdings yet.");
        }
        double largest = holdings.stream().mapToDouble(Holding::allocationPct).max().orElse(0);
        // Herfindahl index: sum of squared allocation fractions (1 = all in one stock).
        double hhi = holdings.stream()
                .mapToDouble(h -> Math.pow(h.allocationPct() / 100.0, 2)).sum();

        String assessment;
        if (largest > 40 || hhi > 0.5) assessment = "High concentration — consider diversifying.";
        else if (largest > 25 || hhi > 0.3) assessment = "Moderate concentration.";
        else assessment = "Well diversified.";

        return new RiskView(holdings.size(), round(largest), round(hhi), assessment);
    }

    private String symbolOf(String token) {
        return instruments.findById(token).map(Instrument::getSymbol).orElse(token);
    }

    private double latestPrice(String token) {
        String raw = quoteCache.latest(token);
        if (raw == null) return 0;
        try {
            return json.readTree(raw).path("ltp").asDouble(0);
        } catch (Exception e) {
            return 0;
        }
    }

    private double round(double v) { return Math.round(v * 100.0) / 100.0; }
}
