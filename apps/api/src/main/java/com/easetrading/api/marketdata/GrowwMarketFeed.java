package com.easetrading.api.marketdata;

import com.easetrading.api.broker.BrokerAdapter;
import com.easetrading.api.instrument.Instrument;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Live prices from Groww. Active when easetrading.broker.provider = groww.
 *
 * Groww's real-time stream is only available through its Python SDK, so for now we
 * poll the LTP endpoint, which takes up to 50 stocks per call:
 *   - every 2 s during market hours (Mon–Fri 9:15–15:30 IST)
 *   - every 60 s outside market hours (prices don't move; this just keeps the last close fresh)
 * Everything downstream only depends on QuoteCache.publish(), so swapping in a true
 * streaming bridge later touches this class only.
 */
@Component
@ConditionalOnProperty(name = "easetrading.broker.provider", havingValue = "groww")
public class GrowwMarketFeed {

    private static final Duration OFF_HOURS_EVERY = Duration.ofSeconds(60);

    private final MarketSubscription subscription;
    private final QuoteCache quoteCache;
    private final BrokerAdapter broker;
    private Instant lastPollAt = Instant.EPOCH;
    private String lastError;

    public GrowwMarketFeed(MarketSubscription subscription, QuoteCache quoteCache, BrokerAdapter broker) {
        this.subscription = subscription;
        this.quoteCache = quoteCache;
        this.broker = broker;
    }

    @Scheduled(fixedDelay = 2000, initialDelay = 5000)
    public void poll() {
        Instant now = Instant.now();
        if (!MarketHours.isOpen(now) && Duration.between(lastPollAt, now).compareTo(OFF_HOURS_EVERY) < 0) {
            return;
        }
        List<Instrument> instruments = new ArrayList<>(subscription.all().values());
        if (instruments.isEmpty()) return;
        lastPollAt = now;

        try {
            broker.ltp(instruments).forEach((token, ltp) ->
                    quoteCache.publish(new Tick(token, ltp, now)));
            lastError = null;
        } catch (Exception e) {
            // Log each distinct problem once instead of every 2 seconds.
            if (!java.util.Objects.equals(e.getMessage(), lastError)) {
                System.err.println("[MarketFeed] Groww LTP poll failed: " + e.getMessage());
                lastError = e.getMessage();
            }
        }
    }
}
