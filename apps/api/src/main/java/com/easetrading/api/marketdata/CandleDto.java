package com.easetrading.api.marketdata;

import java.time.Instant;

/**
 * A lightweight, immutable representation of a single price candle (one bar on a
 * chart). Used to move data between the broker adapter, services and the API
 * without exposing JPA entities. OHLCV = Open, High, Low, Close, Volume.
 */
public record CandleDto(
        String token,
        String interval,   // 1m | 3m | 5m | 15m | 1h | 1d
        Instant ts,        // bar start time
        double open,
        double high,
        double low,
        double close,
        long volume
) {}
