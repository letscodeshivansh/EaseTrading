package com.easetrading.api.marketdata;

import com.easetrading.api.broker.mock.MockPrices;
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
 * the whole app (chart, watchlist, WebSocket) works WITHOUT broker credentials.
 *
 * Active when easetrading.broker.provider = mock (the default). Switch to "groww"
 * and {@link GrowwMarketFeed} takes over with zero other changes.
 */
@Component
@ConditionalOnProperty(name = "easetrading.broker.provider", havingValue = "mock", matchIfMissing = true)
public class MockMarketFeed {

    private final MarketSubscription subscription;
    private final QuoteCache quoteCache;

    // Remembers each token's last price so the walk is continuous, not jumpy.
    private final Map<String, Double> lastPrice = new ConcurrentHashMap<>();

    public MockMarketFeed(MarketSubscription subscription, QuoteCache quoteCache) {
        this.subscription = subscription;
        this.quoteCache = quoteCache;
    }

    /** Emits one tick per subscribed instrument, ~4 times a second for a smooth chart. */
    @Scheduled(fixedRate = 250)
    public void emitTicks() {
        for (Instrument instrument : subscription.all().values()) {
            String token = instrument.getToken();
            // Starts from the same base price the mock candle history ends at.
            double prev = lastPrice.getOrDefault(token, MockPrices.base(token));

            // Random walk: nudge price by up to +/-0.15%, keep it positive.
            double driftPct = ThreadLocalRandom.current().nextDouble(-0.0015, 0.0015);
            double next = Math.max(1.0, prev * (1 + driftPct));
            lastPrice.put(token, next);

            quoteCache.publish(new Tick(token, round2(next), Instant.now()));
        }
    }

    private double round2(double v) { return Math.round(v * 100.0) / 100.0; }
}
