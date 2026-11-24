package com.easetrading.api.broker;

import java.time.Instant;
import java.util.List;

/**
 * Broker-neutral data shapes. Every broker adapter converts its own JSON into these,
 * so the rest of the app only ever sees one format.
 */
public final class BrokerModels {

    private BrokerModels() { }

    /** Connection state shown on the dashboard. message explains what to do when not connected. */
    public record BrokerStatus(String provider, boolean connected, Instant tokenExpiresAt, String message) {}

    public record BrokerProfile(String provider, String clientId, boolean nseEnabled, boolean bseEnabled,
                                List<String> activeSegments) {}

    public record BrokerFunds(double availableCash, double marginUsed, double cncAvailable,
                              double brokerageAndCharges) {}

    /** One demat holding. quantity includes T1 (bought, not yet settled) shares. */
    public record BrokerHolding(String tradingSymbol, String isin, int quantity, double averagePrice,
                                int t1Quantity) {}

    public record BrokerPosition(String tradingSymbol, String exchange, String product, int quantity,
                                 double netPrice, double realisedPnl) {}

    /** Day OHLC. close is the previous close when the broker provides it that way. */
    public record DayOhlc(double open, double high, double low, double close) {}

    public record BrokerQuote(double lastPrice, Double dayChange, Double dayChangePct,
                              Double open, Double high, Double low, Double close,
                              Long volume, Double week52High, Double week52Low,
                              Double marketCap, Double upperCircuit, Double lowerCircuit,
                              Instant lastTradeTime) {}
}
