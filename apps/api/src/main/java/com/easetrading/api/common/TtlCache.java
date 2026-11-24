package com.easetrading.api.common;

import java.time.Duration;
import java.time.Instant;
import java.util.function.Supplier;

/**
 * Remembers one value for a short time. Used to avoid calling the broker again when
 * several screens (or tabs) ask for the same data within a few seconds — protects
 * the broker's rate limits and keeps pages fast.
 */
public final class TtlCache<T> {

    private final Duration ttl;
    private T value;
    private Instant loadedAt;

    public TtlCache(Duration ttl) {
        this.ttl = ttl;
    }

    public synchronized T get(Supplier<T> loader) {
        if (value == null || loadedAt == null || Instant.now().isAfter(loadedAt.plus(ttl))) {
            value = loader.get();
            loadedAt = Instant.now();
        }
        return value;
    }

    public synchronized void clear() {
        value = null;
        loadedAt = null;
    }
}
