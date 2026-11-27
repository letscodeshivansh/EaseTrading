package com.easetrading.api.broker.mock;

/**
 * Starting prices for simulated data. The mock feed AND the mock candle history both
 * start from these, so the chart's last candle and the live price agree (fixes B8).
 */
public final class MockPrices {

    private MockPrices() { }

    public static double base(String token) {
        return switch (token) {
            case "2885" -> 1420;   // RELIANCE
            case "11536" -> 3850;  // TCS
            case "1333" -> 1650;   // HDFCBANK
            case "4963" -> 1180;   // ICICIBANK
            case "1594" -> 1550;   // INFY
            case "3045" -> 790;    // SBIN
            case "10999" -> 11200; // MARUTI
            case "1660" -> 430;    // ITC
            case "3456" -> 760;    // TATAMOTORS
            case "11483" -> 3600;  // LT
            case "NIFTY" -> 24500;
            case "BANKNIFTY" -> 52000;
            case "FINNIFTY" -> 23800;
            case "SENSEX" -> 80800;
            default -> 500 + Math.floorMod(token.hashCode(), 2500);
        };
    }
}
