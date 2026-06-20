package com.easetrading.api.marketdata;

import com.easetrading.api.broker.AngelOneAdapter;
import com.easetrading.api.broker.BrokerSessionProvider;
import com.easetrading.api.config.AppProperties;
import com.easetrading.api.instrument.Instrument;
import com.easetrading.api.instrument.InstrumentRepository;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Provides historical candles for charting.
 *
 * Strategy:
 *  1. If we already have bars in the database, return them (fast path).
 *  2. Otherwise, in LIVE mode fetch them from Angel One and store them.
 *  3. In MOCK mode, generate a believable synthetic history so charts render
 *     immediately during development.
 *
 * This "fetch-once, then serve-from-DB" pattern keeps the broker calls minimal and
 * makes the chart fast.
 */
@Service
public class CandleService {

    private final CandleRepository candles;
    private final InstrumentRepository instruments;
    private final AppProperties props;
    private final AngelOneAdapter angelOne;
    private final BrokerSessionProvider sessionProvider;

    public CandleService(CandleRepository candles, InstrumentRepository instruments,
                         AppProperties props, AngelOneAdapter angelOne,
                         BrokerSessionProvider sessionProvider) {
        this.candles = candles;
        this.instruments = instruments;
        this.props = props;
        this.angelOne = angelOne;
        this.sessionProvider = sessionProvider;
    }

    public List<CandleDto> getCandles(String token, String interval, Instant from, Instant to) {
        // Fast path: already stored.
        if (candles.existsByTokenAndInterval(token, interval)) {
            return candles.findByTokenAndIntervalAndTsBetweenOrderByTsAsc(token, interval, from, to)
                    .stream().map(Candle::toDto).toList();
        }

        // Need to populate this series for the first time.
        List<CandleDto> fetched = props.getMarket().isLive()
                ? fetchFromBroker(token, interval, from, to)
                : generateSynthetic(token, interval, from, to);

        candles.saveAll(fetched.stream().map(Candle::new).toList());
        return fetched.stream()
                .filter(c -> !c.ts().isBefore(from) && !c.ts().isAfter(to))
                .toList();
    }

    private List<CandleDto> fetchFromBroker(String token, String interval, Instant from, Instant to) {
        Instrument inst = instruments.findById(token).orElseThrow();
        // Map our interval names to Angel One's (kept minimal for Prompt 1).
        String angelInterval = switch (interval) {
            case "1m" -> "ONE_MINUTE";
            case "5m" -> "FIVE_MINUTE";
            case "15m" -> "FIFTEEN_MINUTE";
            case "1h" -> "ONE_HOUR";
            default -> "ONE_DAY";
        };
        return angelOne.getCandles(sessionProvider.get(), inst.getExchange(), token, angelInterval, from, to);
    }

    /** Builds a smooth random-walk daily history so the chart looks real in dev. */
    private List<CandleDto> generateSynthetic(String token, String interval, Instant from, Instant to) {
        List<CandleDto> out = new ArrayList<>();
        long days = Math.max(1, ChronoUnit.DAYS.between(from, to));
        double price = 1000 + ThreadLocalRandom.current().nextInt(2500);

        Instant t = from.truncatedTo(ChronoUnit.DAYS);
        for (long i = 0; i < days; i++) {
            double open = price;
            double change = open * ThreadLocalRandom.current().nextDouble(-0.02, 0.02);
            double close = Math.max(1, open + change);
            double high = Math.max(open, close) * (1 + ThreadLocalRandom.current().nextDouble(0, 0.01));
            double low = Math.min(open, close) * (1 - ThreadLocalRandom.current().nextDouble(0, 0.01));
            long vol = ThreadLocalRandom.current().nextLong(100_000, 5_000_000);

            out.add(new CandleDto(token, interval, t,
                    round2(open), round2(high), round2(low), round2(close), vol));
            price = close;
            t = t.plus(1, ChronoUnit.DAYS);
        }
        return out;
    }

    private double round2(double v) { return Math.round(v * 100.0) / 100.0; }
}
