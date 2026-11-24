package com.easetrading.api.instrument;

/**
 * One tradable stock. Held in memory by {@link InstrumentStore} (not a DB table).
 *
 * `token` is the exchange's numeric instrument token (for NSE, e.g. 2885 = RELIANCE —
 * the same number Groww calls `exchange_token`). Every screen, URL and saved row
 * (watchlist, orders, alerts) refers to stocks by this token, so it never changes
 * when the broker changes.
 *
 * Broker-specific names are kept alongside:
 *   tradingSymbol : RELIANCE        (Groww orders, quotes, LTP)
 *   growwSymbol   : NSE-RELIANCE    (Groww historical candles)
 *
 * NSE only for now (decision D1).
 */
public class Instrument {

    private final String token;
    private final String symbol;          // display, e.g. RELIANCE
    private final String name;            // e.g. Reliance Industries Ltd
    private final String exchange;        // NSE | BSE
    private final String segment;         // CASH | FNO
    private String instrumentType = "EQ"; // EQ | IDX | FUT | CE | PE
    private String tradingSymbol;
    private String growwSymbol;
    private String isin;
    private Integer lotSize;
    private Double tickSize;

    public Instrument(String token, String symbol, String name, String exchange, String segment) {
        this.token = token;
        this.symbol = symbol;
        this.name = name;
        this.exchange = exchange;
        this.segment = segment;
        this.tradingSymbol = symbol;
        this.growwSymbol = exchange + "-" + symbol;
    }

    /** Fill in the broker-provided details (used when parsing the broker's instrument file). */
    public Instrument withDetails(String instrumentType, String tradingSymbol, String growwSymbol,
                                  String isin, Integer lotSize, Double tickSize) {
        this.instrumentType = instrumentType;
        this.tradingSymbol = tradingSymbol;
        this.growwSymbol = growwSymbol;
        this.isin = isin;
        this.lotSize = lotSize;
        this.tickSize = tickSize;
        return this;
    }

    public String getToken() { return token; }
    public String getSymbol() { return symbol; }
    public String getName() { return name; }
    public String getExchange() { return exchange; }
    public String getSegment() { return segment; }
    public String getInstrumentType() { return instrumentType; }
    public String getTradingSymbol() { return tradingSymbol != null ? tradingSymbol : symbol; }
    public String getGrowwSymbol() { return growwSymbol != null ? growwSymbol : exchange + "-" + symbol; }
    public String getIsin() { return isin; }
    public Integer getLotSize() { return lotSize; }
    public Double getTickSize() { return tickSize; }
}
