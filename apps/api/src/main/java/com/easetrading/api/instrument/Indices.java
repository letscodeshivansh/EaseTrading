package com.easetrading.api.instrument;

import java.util.List;
import java.util.Optional;

/**
 * The market indices we show and trade options on. They are added to the stock
 * list (InstrumentStore) so they get live prices, charts and quotes like any stock;
 * the token is the index name (Groww uses the same value as the feed token).
 *
 * lotSize / strikeStep are for F&O. Lot sizes are set by the exchange and change
 * from time to time — check before trading.
 */
public final class Indices {

    private Indices() { }

    public record IndexInfo(String token, String symbol, String name, String exchange,
                            int lotSize, int strikeStep) {}

    public static final List<IndexInfo> ALL = List.of(
            new IndexInfo("NIFTY", "NIFTY", "Nifty 50", "NSE", 75, 50),
            new IndexInfo("BANKNIFTY", "BANKNIFTY", "Nifty Bank", "NSE", 35, 100),
            new IndexInfo("FINNIFTY", "FINNIFTY", "Nifty Financial Services", "NSE", 65, 50),
            new IndexInfo("SENSEX", "SENSEX", "BSE Sensex", "BSE", 20, 100));

    public static Optional<IndexInfo> get(String token) {
        return ALL.stream().filter(i -> i.token().equalsIgnoreCase(token)).findFirst();
    }

    /** As stock-list rows (instrument type IDX). */
    public static List<Instrument> asInstruments() {
        return ALL.stream()
                .map(i -> new Instrument(i.token(), i.symbol(), i.name(), i.exchange(), "CASH")
                        .withDetails("IDX", i.symbol(), i.exchange() + "-" + i.symbol(), null, 1, 0.05))
                .toList();
    }
}
