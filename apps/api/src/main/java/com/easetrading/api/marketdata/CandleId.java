package com.easetrading.api.marketdata;

import java.io.Serializable;
import java.time.Instant;
import java.util.Objects;

/**
 * Composite primary key for {@link Candle}. JPA requires a separate class that
 * implements Serializable and equals/hashCode for multi-column keys.
 */
public class CandleId implements Serializable {
    private String token;
    private String interval;
    private Instant ts;

    public CandleId() { }
    public CandleId(String token, String interval, Instant ts) {
        this.token = token; this.interval = interval; this.ts = ts;
    }

    @Override public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof CandleId that)) return false;
        return Objects.equals(token, that.token)
                && Objects.equals(interval, that.interval)
                && Objects.equals(ts, that.ts);
    }

    @Override public int hashCode() { return Objects.hash(token, interval, ts); }
}
