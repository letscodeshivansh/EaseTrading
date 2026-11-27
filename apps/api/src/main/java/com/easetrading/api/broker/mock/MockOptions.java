package com.easetrading.api.broker.mock;

import com.easetrading.api.broker.BrokerModels.*;
import com.easetrading.api.instrument.Indices.IndexInfo;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Simulated F&O data for demo mode: realistic expiries and an option chain priced
 * with Black-Scholes (premiums, Greeks and IV behave like the real thing; open
 * interest is a believable, repeatable pattern — more call OI above the price, more
 * put OI below it).
 */
final class MockOptions {

    private MockOptions() { }

    private static final double RATE = 0.065;          // risk-free rate
    private static final int STRIKES_EACH_SIDE = 12;

    /** NIFTY: weekly (Tuesday). SENSEX: weekly (Thursday). Others: monthly (last Tuesday). */
    static List<LocalDate> expiries(IndexInfo idx, LocalDate today) {
        List<LocalDate> out = new ArrayList<>();
        if (idx.token().equals("NIFTY") || idx.token().equals("SENSEX")) {
            DayOfWeek day = idx.token().equals("NIFTY") ? DayOfWeek.TUESDAY : DayOfWeek.THURSDAY;
            LocalDate d = today.with(TemporalAdjusters.nextOrSame(day));
            for (int i = 0; i < 5; i++) out.add(d.plusWeeks(i));
        } else {
            for (int m = 0; out.size() < 3; m++) {
                LocalDate last = today.plusMonths(m).with(TemporalAdjusters.lastInMonth(DayOfWeek.TUESDAY));
                if (!last.isBefore(today)) out.add(last);
            }
        }
        return out;
    }

    static OptionChainData chain(IndexInfo idx, double spot, LocalDate expiry, LocalDate today) {
        int step = idx.strikeStep();
        double atm = Math.round(spot / step) * (double) step;
        double years = Math.max(0.5, expiry.toEpochDay() - today.toEpochDay()) / 365.0;
        Random rnd = new Random((idx.token() + expiry).hashCode());
        long oiScale = 4_000_000L / Math.max(1, idx.lotSize());

        List<OptionStrike> rows = new ArrayList<>();
        for (int i = -STRIKES_EACH_SIDE; i <= STRIKES_EACH_SIDE; i++) {
            double k = atm + i * step;
            double iv = 0.13 + 0.0025 * Math.abs(i);       // simple volatility "smile"
            double bell = Math.exp(-Math.pow(i / 6.0, 2));
            long callOi = Math.round(oiScale * bell * (i >= 0 ? 1.0 : 0.35) * (0.7 + rnd.nextDouble() * 0.6));
            long putOi = Math.round(oiScale * bell * (i <= 0 ? 1.0 : 0.35) * (0.7 + rnd.nextDouble() * 0.6));
            rows.add(new OptionStrike(k,
                    leg(idx, expiry, k, "CE", spot, years, iv, callOi, rnd),
                    leg(idx, expiry, k, "PE", spot, years, iv, putOi, rnd)));
        }
        return new OptionChainData(round(spot), rows);
    }

    private static OptionLeg leg(IndexInfo idx, LocalDate expiry, double k, String type, double s,
                                 double t, double iv, long oi, Random rnd) {
        boolean call = type.equals("CE");
        double d1 = (Math.log(s / k) + (RATE + iv * iv / 2) * t) / (iv * Math.sqrt(t));
        double d2 = d1 - iv * Math.sqrt(t);
        double disc = Math.exp(-RATE * t);
        double price = call ? s * cdf(d1) - k * disc * cdf(d2) : k * disc * cdf(-d2) - s * cdf(-d1);
        double delta = call ? cdf(d1) : cdf(d1) - 1;
        double gamma = pdf(d1) / (s * iv * Math.sqrt(t));
        double thetaYear = -s * pdf(d1) * iv / (2 * Math.sqrt(t))
                - (call ? RATE * k * disc * cdf(d2) : -RATE * k * disc * cdf(-d2));
        double vega = s * pdf(d1) * Math.sqrt(t) / 100;
        String symbol = idx.symbol() + expiry.toString().replace("-", "").substring(2) + (long) k + type;
        return new OptionLeg(symbol, round(Math.max(0.05, price)), oi,
                Math.round(oi * (0.3 + rnd.nextDouble() * 0.9)),
                new OptionGreeks(r4(delta), r4(gamma), round(thetaYear / 365), round(vega), round(iv * 100)));
    }

    /** Standard normal CDF (Abramowitz–Stegun approximation, error < 1e-7). */
    static double cdf(double x) {
        double t = 1 / (1 + 0.2316419 * Math.abs(x));
        double poly = t * (0.319381530 + t * (-0.356563782 + t * (1.781477937 + t * (-1.821255978 + t * 1.330274429))));
        double tail = pdf(x) * poly;
        return x >= 0 ? 1 - tail : tail;
    }

    static double pdf(double x) {
        return Math.exp(-x * x / 2) / Math.sqrt(2 * Math.PI);
    }

    private static double round(double v) { return Math.round(v * 100.0) / 100.0; }
    private static double r4(double v) { return Math.round(v * 10_000.0) / 10_000.0; }
}
