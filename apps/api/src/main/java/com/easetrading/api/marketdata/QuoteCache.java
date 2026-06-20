package com.easetrading.api.marketdata;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * The single place that records a live price. Both feeds call publish(tick); after
 * that the value is (a) cached in Redis for instant reads and (b) broadcast to every
 * connected browser via the Redis "ticks" pub/sub channel.
 */
@Component
public class QuoteCache {

    /** Redis channel name that the WebSocket handler listens on. */
    public static final String TICKS_CHANNEL = "chan:ticks";

    private final StringRedisTemplate redis;
    private final ObjectMapper json = new ObjectMapper();

    public QuoteCache(StringRedisTemplate redis) {
        this.redis = redis;
    }

    /** Store the latest price and notify subscribers. */
    public void publish(Tick tick) {
        try {
            String payload = json.writeValueAsString(tick);
            // 1) Hot cache: latest LTP per token, expires if no updates for a while.
            redis.opsForValue().set("quote:" + tick.token(), payload, Duration.ofMinutes(10));
            // 2) Fan-out: anyone subscribed to the channel (our WebSocket handler) gets it.
            redis.convertAndSend(TICKS_CHANNEL, payload);
        } catch (Exception e) {
            // A single bad tick must never crash the feed loop.
            System.err.println("Failed to publish tick: " + e.getMessage());
        }
    }

    /** Latest cached price for one token, or null if we have none yet. */
    public String latest(String token) {
        return redis.opsForValue().get("quote:" + token);
    }
}
