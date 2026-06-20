package com.easetrading.api.instrument;

import jakarta.persistence.*;

/**
 * The "symbol master": one row per tradable instrument on NSE/BSE.
 * Angel One identifies instruments by a numeric "token"; humans use the symbol
 * (e.g. RELIANCE). We store both so we can translate between them.
 *
 * In production this table is refreshed daily from Angel One's scrip-master file.
 * For Prompt 1 we seed a handful of well-known symbols so search works immediately.
 */
@Entity
@Table(name = "instrument", indexes = {
        @Index(name = "idx_instrument_symbol", columnList = "symbol,exchange")
})
public class Instrument {

    @Id
    private String token;            // Angel One instrument token (primary key)

    @Column(nullable = false)
    private String symbol;           // e.g. RELIANCE

    private String name;             // e.g. Reliance Industries Ltd

    @Column(nullable = false)
    private String exchange;         // NSE | BSE

    private String segment;          // EQ | FUT | OPT ...
    private String isin;
    private Integer lotSize;
    private Double tickSize;
    private String sector;

    protected Instrument() { }

    public Instrument(String token, String symbol, String name, String exchange, String segment) {
        this.token = token;
        this.symbol = symbol;
        this.name = name;
        this.exchange = exchange;
        this.segment = segment;
    }

    public String getToken() { return token; }
    public String getSymbol() { return symbol; }
    public String getName() { return name; }
    public String getExchange() { return exchange; }
    public String getSegment() { return segment; }
    public String getSector() { return sector; }
}
