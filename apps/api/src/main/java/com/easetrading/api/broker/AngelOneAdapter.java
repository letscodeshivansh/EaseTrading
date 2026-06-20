package com.easetrading.api.broker;

import com.easetrading.api.marketdata.CandleDto;

import java.time.Instant;
import java.util.List;

/**
 * The single interface behind ALL Angel One SmartAPI calls.
 *
 * Why an interface? It isolates the rest of the app from the broker's quirks.
 * If Angel One changes its API (or we add another broker like Zerodha), only the
 * implementation changes — controllers and services stay untouched. It also lets
 * us swap in a fake implementation during tests.
 */
public interface AngelOneAdapter {

    /**
     * Logs in using the configured default credentials and returns session tokens.
     * (In a multi-user build this would take per-user credentials.)
     */
    AngelOneSession login();

    /**
     * Fetches historical candles for one instrument.
     * Angel One allows up to 8,000 candles per request.
     *
     * @param exchange NSE | BSE
     * @param token    Angel One instrument token
     * @param interval ONE_MINUTE | FIVE_MINUTE | ONE_DAY ... (SmartAPI naming)
     */
    List<CandleDto> getCandles(AngelOneSession session, String exchange, String token,
                               String interval, Instant from, Instant to);

    /**
     * Returns the latest traded price for one instrument.
     * Used by the "live" feed (REST polling) to stream prices to the dashboard.
     */
    double getLastTradedPrice(AngelOneSession session, String exchange, String symbol, String token);

    /**
     * Places a real order with Angel One and returns the broker's order id.
     * Only ever called by LiveTradeExecutor, after risk checks and human confirmation.
     *
     * @param side BUY | SELL
     * @param type MARKET | LIMIT
     */
    String placeOrder(AngelOneSession session, String exchange, String symbol, String token,
                      String side, String type, int qty, double price);
}
