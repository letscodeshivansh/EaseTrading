package com.easetrading.api.marketdata;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

/**
 * The single place that records a live price — IN MEMORY (no Redis).
 *
 * Both feeds call publish(tick); the latest price per stock is kept in a map for
 * instant reads, and every listener (the WebSocket handler) is told about it.
 * Prices start empty after a restart and refill within seconds from the feed, which
 * also means demo prices can never leak into real mode (old bug B23).
 */
@Component
public class QuoteCache {

    // JavaTimeModule is required to write Tick.ts (an Instant) — see bug B22.
    private final ObjectMapper json = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    private final Map<String, Tick> latest = new ConcurrentHashMap<>();
    private final List<Consumer<String>> listeners = new CopyOnWriteArrayList<>();

    /** Register someone who wants every tick as JSON (e.g. the WebSocket handler). */
    public void addListener(Consumer<String> listener) {
        listeners.add(listener);
    }

    /** Store the latest price and notify listeners. */
    public void publish(Tick tick) {
        latest.put(tick.token(), tick);
        try {
            String payload = json.writeValueAsString(tick);
            for (Consumer<String> l : listeners) l.accept(payload);
        } catch (Exception e) {
            // A single bad tick must never crash the feed loop.
            System.err.println("Failed to publish tick: " + e.getMessage());
        }
    }

    /** Latest tick as JSON ({token, ltp, ts}) for one token, or null if we have none yet. */
    public String latest(String token) {
        Tick t = latest.get(token);
        if (t == null) return null;
        try {
            return json.writeValueAsString(t);
        } catch (Exception e) {
            return null;
        }
    }

    /** Latest price for one token, or 0 if unknown. */
    public double latestPrice(String token) {
        Tick t = latest.get(token);
        return t == null ? 0 : t.ltp();
    }
}
