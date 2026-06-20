package com.easetrading.api.marketdata;

import com.easetrading.api.instrument.Instrument;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Development market feed: generates realistic-looking ticks with a random walk so
 * the whole app (chart, watchlist, WebSocket) works WITHOUT Angel One credentials.
 *
 * Active only when easetrading.market.feed-mode = mock (the default). Swap to "live"
 * and {@link AngelOneMarketFeed} takes over with zero other changes.
 */
@Component
@ConditionalOnProperty(name = "easetrading.market.feed-mode", havingValue = "mock", matchIfMissing = true)
public class MockMarketFeed {

    private final MarketSubscription subscription;
    private final QuoteCache quoteCache;

    // Remembers each token's last price so the walk is continuous, not jumpy.
    private final Map<String, Double> lastPrice = new ConcurrentHashMap<>();

    public MockMarketFeed(MarketSubscription subscription, QuoteCache quoteCache) {
        this.subscription = subscription;
        this.quoteCache = quoteCache;
        System.out.println("[MarketFeed] MOCK feed active — generating simulated ticks.");
    }

    /** Emits one tick per subscribed instrument, ~4 times a second for a smooth chart. */
    @Scheduled(fixedRate = 250)
    public void emitTicks() {
        for (Instrument instrument : subscription.all().values()) {
            String token = instrument.getToken();
            double prev = lastPrice.getOrDefault(token, seedPriceFor(token));

            // Random walk: nudge price by up to +/-0.15%, keep it positive.
            double driftPct = ThreadLocalRandom.current().nextDouble(-0.0015, 0.0015);
            double next = Math.max(1.0, prev * (1 + driftPct));
            lastPrice.put(token, next);

            quoteCache.publish(new Tick(token, round2(next), Instant.now()));
        }
    }

    /** A plausible starting price so different stocks look different on screen. */
    private double seedPriceFor(String token) {
        return switch (token) {
            case "2885" -> 1420;   // RELIANCE
            case "11536" -> 3850;  // TCS
            case "1333" -> 1650;   // HDFCBANK
            case "4963" -> 1180;   // ICICIBANK
            case "1594" -> 1550;   // INFY
            default -> 1000 + ThreadLocalRandom.current().nextInt(500);
        };
    }

    private double round2(double v) { return Math.round(v * 100.0) / 100.0; }
}
