package com.easetrading.api.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Strongly-typed holder for all "easetrading.*" settings from application.yml.
 * Using a single typed object (instead of scattered @Value strings) keeps
 * configuration discoverable and refactor-safe.
 */
@Component
@ConfigurationProperties(prefix = "easetrading")
public class AppProperties {

    private final Security security = new Security();
    private final Market market = new Market();
    private final AngelOne angelone = new AngelOne();
    private final Analysis analysis = new Analysis();
    private final Trading trading = new Trading();
    private final Risk risk = new Risk();

    public Security getSecurity() { return security; }
    public Market getMarket() { return market; }
    public AngelOne getAngelone() { return angelone; }
    public Analysis getAnalysis() { return analysis; }
    public Trading getTrading() { return trading; }
    public Risk getRisk() { return risk; }

    /** JWT signing + credential encryption settings. */
    public static class Security {
        private String jwtSecret;
        private long jwtExpiryMinutes = 120;
        private String credentialAesKey; // base64-encoded 32-byte key
        public String getJwtSecret() { return jwtSecret; }
        public void setJwtSecret(String v) { this.jwtSecret = v; }
        public long getJwtExpiryMinutes() { return jwtExpiryMinutes; }
        public void setJwtExpiryMinutes(long v) { this.jwtExpiryMinutes = v; }
        public String getCredentialAesKey() { return credentialAesKey; }
        public void setCredentialAesKey(String v) { this.credentialAesKey = v; }
    }

    /** Market-data feed selection. */
    public static class Market {
        private String feedMode = "mock"; // "mock" | "live"
        public String getFeedMode() { return feedMode; }
        public void setFeedMode(String v) { this.feedMode = v; }
        public boolean isLive() { return "live".equalsIgnoreCase(feedMode); }
    }

    /** Default Angel One credentials for single-user dev (per-user in production). */
    public static class AngelOne {
        private String apiKey;
        private String clientId;
        private String password;
        private String totpSecret;
        private String baseUrl = "https://apiconnect.angelone.in";
        public String getApiKey() { return apiKey; }
        public void setApiKey(String v) { this.apiKey = v; }
        public String getClientId() { return clientId; }
        public void setClientId(String v) { this.clientId = v; }
        public String getPassword() { return password; }
        public void setPassword(String v) { this.password = v; }
        public String getTotpSecret() { return totpSecret; }
        public void setTotpSecret(String v) { this.totpSecret = v; }
        public String getBaseUrl() { return baseUrl; }
        public void setBaseUrl(String v) { this.baseUrl = v; }
    }

    /** Where the Python analysis service lives (used in Prompts 2/3). */
    public static class Analysis {
        private String serviceUrl = "http://localhost:8000";
        public String getServiceUrl() { return serviceUrl; }
        public void setServiceUrl(String v) { this.serviceUrl = v; }
    }

    /** How orders are executed: "paper" (simulated, safe) or "live" (real money). */
    public static class Trading {
        private String mode = "paper";
        public String getMode() { return mode; }
        public void setMode(String v) { this.mode = v; }
        public boolean isLive() { return "live".equalsIgnoreCase(mode); }
    }

    /** Pre-trade risk limits. Every order is checked against these before submission. */
    public static class Risk {
        private double maxOrderValue = 100_000;   // max rupees per single order
        private double maxPositionPct = 25;        // max % of portfolio in one stock
        private double dailyLossLimit = 25_000;    // block new buys after this day loss
        private double priceBandPct = 10;          // limit price must be within +/-X% of LTP
        public double getMaxOrderValue() { return maxOrderValue; }
        public void setMaxOrderValue(double v) { this.maxOrderValue = v; }
        public double getMaxPositionPct() { return maxPositionPct; }
        public void setMaxPositionPct(double v) { this.maxPositionPct = v; }
        public double getDailyLossLimit() { return dailyLossLimit; }
        public void setDailyLossLimit(double v) { this.dailyLossLimit = v; }
        public double getPriceBandPct() { return priceBandPct; }
        public void setPriceBandPct(double v) { this.priceBandPct = v; }
    }
}
