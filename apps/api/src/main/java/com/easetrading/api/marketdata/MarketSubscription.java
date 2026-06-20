package com.easetrading.api.marketdata;

import com.easetrading.api.instrument.Instrument;
import com.easetrading.api.instrument.InstrumentRepository;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * The set of instruments currently being streamed. Both feeds (mock + live) read
 * from this; the WebSocket handler adds/removes tokens as clients subscribe.
 *
 * Backed by a concurrent map because background feed threads and request threads
 * touch it simultaneously.
 */
@Component
public class MarketSubscription {

    private final InstrumentRepository instruments;
    // token -> Instrument (so the live feed knows the symbol/exchange to poll)
    private final Map<String, Instrument> subscribed = new ConcurrentHashMap<>();

    public MarketSubscription(InstrumentRepository instruments) {
        this.instruments = instruments;
    }

    /** Seed a default watchlist so the dashboard streams something immediately. */
    @PostConstruct
    void seedDefault() {
        instruments.findById("2885").ifPresent(this::add);  // RELIANCE
        instruments.findById("11536").ifPresent(this::add); // TCS
        instruments.findById("1333").ifPresent(this::add);  // HDFCBANK
    }

    public void add(Instrument instrument) { subscribed.put(instrument.getToken(), instrument); }

    public void subscribeToken(String token) {
        instruments.findById(token).ifPresent(this::add);
    }

    public void remove(String token) { subscribed.remove(token); }

    public Map<String, Instrument> all() { return subscribed; }

    public Optional<Instrument> get(String token) { return Optional.ofNullable(subscribed.get(token)); }
}
