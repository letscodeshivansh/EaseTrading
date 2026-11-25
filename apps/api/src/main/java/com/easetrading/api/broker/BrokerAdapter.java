package com.easetrading.api.broker;

import com.easetrading.api.broker.BrokerModels.*;
import com.easetrading.api.instrument.Instrument;
import com.easetrading.api.marketdata.CandleDto;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * The ONE interface the rest of the app uses to talk to a broker.
 *
 * Controllers and services never know whether they're talking to Groww or to the
 * mock. Adding another broker later = one new implementation of this interface,
 * nothing else changes. The active implementation is chosen by
 * easetrading.broker.provider (mock | groww).
 *
 * Order placement is intentionally NOT here yet — it arrives in phase G5, after the
 * safety fixes. Until then the app can only READ from the broker.
 */
public interface BrokerAdapter {

    /** "MOCK" or "GROWW" — also stored on candles so data from different sources never mixes. */
    String source();

    /** Is the broker reachable with a valid session right now? Never throws. */
    BrokerStatus status();

    /** Try logging in again now (the "Reconnect" button). */
    void reconnect();

    /** Account basics (client code, enabled exchanges/segments). */
    BrokerProfile profile();

    /** Cash and margin available. */
    BrokerFunds funds();

    /** Long-term holdings in the demat account. */
    List<BrokerHolding> holdings();

    /** Today's intraday / carry-forward positions. */
    List<BrokerPosition> positions();

    /** Last traded price for many instruments at once. Key = instrument token. */
    Map<String, Double> ltp(List<Instrument> instruments);

    /** Day OHLC snapshot for many instruments (close = previous close where the broker provides it). */
    Map<String, DayOhlc> ohlc(List<Instrument> instruments);

    /** Detailed quote for one instrument (day change, 52-week range, volume…). */
    BrokerQuote quote(Instrument instrument);

    /**
     * Historical candles. interval uses our names: 1m | 5m | 15m | 1h | 1d | 1w.
     * Implementations handle any per-request range limits themselves.
     */
    List<CandleDto> candles(Instrument instrument, String interval, Instant from, Instant to);
}
