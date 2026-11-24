package com.easetrading.api.marketdata;

import com.easetrading.api.broker.BrokerAdapter;
import com.easetrading.api.common.ApiException;
import com.easetrading.api.instrument.Instrument;
import com.easetrading.api.instrument.InstrumentStore;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Historical candles for charts, indicators and analysis — fetched from the broker
 * and kept in a small IN-MEMORY cache (no database).
 *
 * The broker already stores the history; we only remember the last answer for a
 * short time so opening the same chart twice doesn't call the broker twice:
 *   daily/weekly bars : 15 minutes
 *   intraday bars     : 1 minute
 * If a wider date range is asked for than what's cached, we fetch again.
 * At most 300 series are kept (least recently used are dropped).
 */
@Service
public class CandleService {

    private static final int MAX_SERIES = 300;
    private static final Duration DAILY_TTL = Duration.ofMinutes(15);
    private static final Duration INTRADAY_TTL = Duration.ofMinutes(1);
    /** A cached series that starts within this of the requested start still counts as covering it. */
    private static final Duration START_SLACK = Duration.ofDays(5);

    private record Entry(Instant from, Instant fetchedAt, List<CandleDto> bars) {}

    private final InstrumentStore instruments;
    private final BrokerAdapter broker;

    /** LRU map: access order + eviction of the eldest entry. */
    private final Map<String, Entry> cache = new LinkedHashMap<>(64, 0.75f, true) {
        @Override
        protected boolean removeEldestEntry(Map.Entry<String, Entry> eldest) {
            return size() > MAX_SERIES;
        }
    };

    public CandleService(InstrumentStore instruments, BrokerAdapter broker) {
        this.instruments = instruments;
        this.broker = broker;
    }

    public List<CandleDto> getCandles(String token, String interval, Instant from, Instant to) {
        String key = token + "|" + interval;
        Duration ttl = interval.endsWith("d") || interval.endsWith("w") ? DAILY_TTL : INTRADAY_TTL;

        Entry hit;
        synchronized (cache) {
            hit = cache.get(key);
        }
        boolean fresh = hit != null
                && hit.fetchedAt().plus(ttl).isAfter(Instant.now())
                && !hit.from().isAfter(from.plus(START_SLACK));

        if (!fresh) {
            Instrument inst = instruments.get(token)
                    .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Unknown instrument: " + token));
            Instant fetchFrom = hit != null && hit.from().isBefore(from) ? hit.from() : from;
            try {
                hit = new Entry(fetchFrom, Instant.now(), broker.candles(inst, interval, fetchFrom, Instant.now()));
                synchronized (cache) {
                    cache.put(key, hit);
                }
            } catch (Exception e) {
                // Serve the last good answer (if any) rather than failing the chart.
                System.err.println("[Candles] fetch failed for " + token + ": " + e.getMessage());
                if (hit == null) return List.of();
            }
        }

        return hit.bars().stream()
                .filter(c -> !c.ts().isBefore(from) && !c.ts().isAfter(to))
                .toList();
    }
}
