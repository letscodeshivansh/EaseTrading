package com.easetrading.api.marketdata;

import java.time.Instant;

/**
 * A single live price update for one instrument.
 * This is the small JSON payload pushed to the browser over the WebSocket.
 */
public record Tick(String token, double ltp, Instant ts) {}
