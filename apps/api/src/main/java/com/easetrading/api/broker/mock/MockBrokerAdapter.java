package com.easetrading.api.broker.mock;

import com.easetrading.api.broker.BrokerAdapter;
import com.easetrading.api.broker.BrokerModels.*;
import com.easetrading.api.instrument.Instrument;
import com.easetrading.api.marketdata.CandleDto;
import com.easetrading.api.marketdata.QuoteCache;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.*;

/**
 * Simulated broker so the whole app works with NO credentials (the default).
 * Active when easetrading.broker.provider = mock (or unset).
 *
 * Everything is deterministic per stock (same stock -> same history), and prices
 * line up with the mock live feed.
 */
@Component
@ConditionalOnProperty(name = "easetrading.broker.provider", havingValue = "mock", matchIfMissing = true)
public class MockBrokerAdapter implements BrokerAdapter {

    private static final ZoneId IST = ZoneId.of("Asia/Kolkata");

    private final QuoteCache quotes;

    public MockBrokerAdapter(QuoteCache quotes) {
        this.quotes = quotes;
        System.out.println("[Broker] MOCK — simulated market data and portfolio.");
    }

    @Override
    public String source() { return "MOCK"; }

    @Override
    public BrokerStatus status() {
        return new BrokerStatus("MOCK", true, null, "Simulated data — set BROKER_PROVIDER=groww for your real account");
    }

    @Override
    public BrokerProfile profile() {
        return new BrokerProfile("MOCK", "DEMO0001", true, true, List.of("CASH"));
    }

    @Override
    public BrokerFunds funds() {
        return new BrokerFunds(100_000, 0, 100_000, 0);
    }

    @Override
    public List<BrokerHolding> holdings() {
        return List.of(
                new BrokerHolding("RELIANCE", "INE002A01018", 10, 1350.0, 0),
                new BrokerHolding("TCS", "INE467B01029", 4, 3620.0, 0),
                new BrokerHolding("HDFCBANK", "INE040A01034", 12, 1580.0, 0),
                new BrokerHolding("INFY", "INE009A01021", 8, 1610.0, 0));
    }

    @Override
    public List<BrokerPosition> positions() {
        return List.of();
    }

    @Override
    public Map<String, Double> ltp(List<Instrument> instruments) {
        Map<String, Double> out = new HashMap<>();
        for (Instrument i : instruments) {
            double live = quotes.latestPrice(i.getToken());
            out.put(i.getToken(), live > 0 ? live : MockPrices.base(i.getToken()));
        }
        return out;
    }

    @Override
    public Map<String, DayOhlc> ohlc(List<Instrument> instruments) {
        Map<String, DayOhlc> out = new HashMap<>();
        Map<String, Double> ltp = ltp(instruments);
        for (Instrument i : instruments) {
            double prevClose = MockPrices.base(i.getToken());
            double last = ltp.get(i.getToken());
            out.put(i.getToken(), new DayOhlc(prevClose, Math.max(prevClose, last), Math.min(prevClose, last), prevClose));
        }
        return out;
    }

    @Override
    public BrokerQuote quote(Instrument instrument) {
        double base = MockPrices.base(instrument.getToken());
        double last = ltp(List.of(instrument)).get(instrument.getToken());
        double change = last - base;
        return new BrokerQuote(round(last), round(change), round(change / base * 100),
                base, Math.max(base, last), Math.min(base, last), base,
                1_250_000L, round(base * 1.25), round(base * 0.78), null,
                round(base * 1.2), round(base * 0.8), Instant.now());
    }

    /**
     * Synthetic daily candles that END at the stock's base price (so the chart joins
     * the live mock feed smoothly). Seeded per token: same stock, same history.
     */
    @Override
    public List<CandleDto> candles(Instrument instrument, String interval, Instant from, Instant to) {
        String token = instrument.getToken();
        LocalDate start = from.atZone(IST).toLocalDate();
        LocalDate end = to.atZone(IST).toLocalDate();
        long days = Math.max(1, ChronoUnit.DAYS.between(start, end) + 1);

        Random rnd = new Random(token.hashCode());
        // Walk backwards from today's base price.
        double[] closes = new double[(int) days];
        double price = MockPrices.base(token);
        for (int i = (int) days - 1; i >= 0; i--) {
            closes[i] = price;
            price = Math.max(1, price / (1 + (rnd.nextDouble() * 0.04 - 0.02)));
        }

        List<CandleDto> out = new ArrayList<>();
        for (int i = 0; i < days; i++) {
            LocalDate d = start.plusDays(i);
            double close = closes[i];
            double open = i == 0 ? close : closes[i - 1];
            double high = Math.max(open, close) * (1 + rnd.nextDouble() * 0.01);
            double low = Math.min(open, close) * (1 - rnd.nextDouble() * 0.01);
            long vol = 100_000 + (long) (rnd.nextDouble() * 4_900_000);
            out.add(new CandleDto(token, interval, d.atStartOfDay(IST).toInstant(),
                    round(open), round(high), round(low), round(close), vol));
        }
        return out;
    }

    private static double round(double v) { return Math.round(v * 100.0) / 100.0; }
}
