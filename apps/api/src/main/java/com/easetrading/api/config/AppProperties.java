package com.easetrading.api.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Strongly-typed holder for all "easetrading.*" settings from application.yml.
 * Using a single typed object (instead of scattered @Value strings) keeps
 * configuration discoverable and refactor-safe.
 *
 * Secrets (Groww key/secret/token, JWT secret) come ONLY from environment
 * variables (see .env, which is git-ignored). Never log these objects.
 */
@Component
@ConfigurationProperties(prefix = "easetrading")
public class AppProperties {

    private final Security security = new Security();
    private final Broker broker = new Broker();
    private final Groww groww = new Groww();
    private final Analysis analysis = new Analysis();
    private final Trading trading = new Trading();
    private final Risk risk = new Risk();

    public Security getSecurity() { return security; }
    public Broker getBroker() { return broker; }
    public Groww getGroww() { return groww; }
    public Analysis getAnalysis() { return analysis; }
    public Trading getTrading() { return trading; }
    public Risk getRisk() { return risk; }

    /** JWT signing settings (app login). */
    public static class Security {
        private String jwtSecret;
        private long jwtExpiryMinutes = 120;
        public String getJwtSecret() { return jwtSecret; }
        public void setJwtSecret(String v) { this.jwtSecret = v; }
        public long getJwtExpiryMinutes() { return jwtExpiryMinutes; }
        public void setJwtExpiryMinutes(long v) { this.jwtExpiryMinutes = v; }
    }

    /**
     * Where market data, stock list and portfolio come from.
     *   mock  = simulated data, no credentials needed (default, safe for dev)
     *   groww = the real Groww Trade API
     */
    public static class Broker {
        private String provider = "mock";
        public String getProvider() { return provider; }
        public void setProvider(String v) { this.provider = v; }
        public boolean isGroww() { return "groww".equalsIgnoreCase(provider); }
    }

    /** Groww Trade API settings. Values come from GROWW_* environment variables. */
    public static class Groww {
        private String baseUrl = "https://api.groww.in";
        private String instrumentsUrl = "https://growwapi-assets.groww.in/instruments/instrument.csv";
        /** token = paste a daily access token | approval = API key + secret | totp = API key + TOTP secret */
        private String authMode = "approval";
        private String accessToken;
        private String apiKey;
        private String apiSecret;
        private String totpSecret;
        public String getBaseUrl() { return baseUrl; }
        public void setBaseUrl(String v) { this.baseUrl = v; }
        public String getInstrumentsUrl() { return instrumentsUrl; }
        public void setInstrumentsUrl(String v) { this.instrumentsUrl = v; }
        public String getAuthMode() { return authMode; }
        public void setAuthMode(String v) { this.authMode = v; }
        public String getAccessToken() { return accessToken; }
        public void setAccessToken(String v) { this.accessToken = v; }
        public String getApiKey() { return apiKey; }
        public void setApiKey(String v) { this.apiKey = v; }
        public String getApiSecret() { return apiSecret; }
        public void setApiSecret(String v) { this.apiSecret = v; }
        public String getTotpSecret() { return totpSecret; }
        public void setTotpSecret(String v) { this.totpSecret = v; }
    }

    /** Where the Python analysis service lives. */
    public static class Analysis {
        private String serviceUrl = "http://localhost:8000";
        public String getServiceUrl() { return serviceUrl; }
        public void setServiceUrl(String v) { this.serviceUrl = v; }
    }

    /** How orders are executed: "paper" (simulated, safe) or "live" (real money — blocked until phase G5). */
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
