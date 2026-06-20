package com.easetrading.api.marketdata;

import jakarta.persistence.*;

import java.time.Instant;

/**
 * A stored price candle. The primary key is (token, interval, ts) so the same
 * instrument can have bars at multiple resolutions (1m, 1d, ...) without clashing.
 *
 * In production this table is a TimescaleDB "hypertable" partitioned on ts, which
 * makes range queries over millions of bars fast. The @IdClass below maps the
 * composite key.
 */
@Entity
@Table(name = "price_candle")
@IdClass(CandleId.class)
public class Candle {

    @Id private String token;
    @Id private String interval;
    @Id private Instant ts;

    private double open;
    private double high;
    private double low;
    private double close;
    private long volume;

    protected Candle() { }

    public Candle(CandleDto d) {
        this.token = d.token();
        this.interval = d.interval();
        this.ts = d.ts();
        this.open = d.open();
        this.high = d.high();
        this.low = d.low();
        this.close = d.close();
        this.volume = d.volume();
    }

    public CandleDto toDto() {
        return new CandleDto(token, interval, ts, open, high, low, close, volume);
    }
}
