package com.easetrading.api.broker;

import com.easetrading.api.broker.BrokerModels.*;
import com.easetrading.api.instrument.Instrument;
import com.easetrading.api.marketdata.CandleDto;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * MARKET DATA from the broker — prices, quotes, history — shared by every user.
 * Implementations: MockBrokerAdapter (demo) or GrowwAdapter, chosen by
 * easetrading.broker.provider (mock | groww).
 *
 * A user's own account data (holdings, funds, positions) is NOT here: it needs
 * that user's own broker login — see AccountService.
 */
public interface BrokerAdapter {

    /** "MOCK" or "GROWW". */
    String source();

    /** Retry the market-data login if it had failed (part of the Reconnect button). */
    void reconnect();

    /** Last traded price for many instruments at once. Key = instrument token. */
    Map<String, Double> ltp(List<Instrument> instruments);

    /** Day OHLC snapshot for many instruments (close = previous close where the broker provides it). */
    Map<String, DayOhlc> ohlc(List<Instrument> instruments);

    /** Detailed quote for one instrument (day change, 52-week range, volume…). */
    BrokerQuote quote(Instrument instrument);

    /** Historical candles. interval: 1m | 5m | 15m | 1h | 1d | 1w. */
    List<CandleDto> candles(Instrument instrument, String interval, Instant from, Instant to);
}
