package com.easetrading.api.instrument;

import org.springframework.stereotype.Service;

import java.util.Optional;

/**
 * Maps a broker's way of naming a stock (trading symbol, ISIN) back to our
 * instrument. Used to join broker holdings with live prices and charts.
 */
@Service
public class InstrumentResolver {

    private final InstrumentStore store;

    public InstrumentResolver(InstrumentStore store) {
        this.store = store;
    }

    public Optional<Instrument> resolve(String tradingSymbol, String isin) {
        if (tradingSymbol != null && !tradingSymbol.isBlank()) {
            Optional<Instrument> bySymbol = store.byTradingSymbol(tradingSymbol, "NSE")
                    .or(() -> store.bySymbol(stripEq(tradingSymbol), "NSE"));
            if (bySymbol.isPresent()) return bySymbol;
        }
        return store.byIsin(isin);
    }

    private static String stripEq(String s) {
        return s.endsWith("-EQ") ? s.substring(0, s.length() - 3) : s;
    }
}
