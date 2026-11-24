package com.easetrading.api.broker.groww;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.Map;

/**
 * Keeps us safely under Groww's published rate limits by spacing calls out per
 * category. Groww's limits (per second / per minute):
 *   Orders      10/s, 250/min
 *   Live data   10/s, 300/min
 *   Non-trading 20/s, 500/min   (holdings, positions, margin, order status)
 *   Undocumented (candles, user, instruments) — we stay conservative.
 *
 * We pace at the per-minute average (e.g. live data 300/min = 5/s) so neither the
 * per-second nor the per-minute limit can be hit. Simple and good enough for one
 * user; swap for a shared Redis bucket if we ever run several api instances.
 */
@Component
@ConditionalOnProperty(name = "easetrading.broker.provider", havingValue = "groww")
public class GrowwRateLimiter {

    public enum Category {
        ORDERS(4), LIVE_DATA(5), NON_TRADING(8), OTHER(3);

        final long minGapMs;
        Category(int perSecond) { this.minGapMs = 1000L / perSecond; }
    }

    private final Map<Category, Long> nextAllowedAt = new EnumMap<>(Category.class);

    /** Blocks briefly if needed so calls in this category are evenly spaced. */
    public void acquire(Category category) {
        long waitMs;
        synchronized (this) {
            long now = System.currentTimeMillis();
            long slot = Math.max(now, nextAllowedAt.getOrDefault(category, 0L));
            nextAllowedAt.put(category, slot + category.minGapMs);
            waitMs = slot - now;
        }
        if (waitMs > 0) {
            try {
                Thread.sleep(waitMs);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }
}
