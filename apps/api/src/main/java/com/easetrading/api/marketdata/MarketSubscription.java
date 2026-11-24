package com.easetrading.api.marketdata;

import com.easetrading.api.instrument.Instrument;
import com.easetrading.api.instrument.InstrumentStore;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * The set of instruments currently being streamed. Both feeds (mock + Groww) read
 * from this. It's filled at startup by StartupBootstrap (default stocks + every
 * watchlist + your holdings) and grows as you add stocks to a watchlist.
 *
 * Backed by a concurrent map because background feed threads and request threads
 * touch it simultaneously.
 */
@Component
public class MarketSubscription {

    private final InstrumentStore instruments;
    // token -> Instrument (so the feed knows the symbol/exchange to poll)
    private final Map<String, Instrument> subscribed = new ConcurrentHashMap<>();

    public MarketSubscription(InstrumentStore instruments) {
        this.instruments = instruments;
    }

    public void add(Instrument instrument) { subscribed.put(instrument.getToken(), instrument); }

    public void subscribeToken(String token) {
        if (token == null || subscribed.containsKey(token)) return;
        instruments.get(token).ifPresent(this::add);
    }

    public void remove(String token) { subscribed.remove(token); }

    public Map<String, Instrument> all() { return subscribed; }

    public Optional<Instrument> get(String token) { return Optional.ofNullable(subscribed.get(token)); }
}
