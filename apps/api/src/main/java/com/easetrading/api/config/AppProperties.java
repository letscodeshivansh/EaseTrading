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

    public Security getSecurity() { return security; }
    public Broker getBroker() { return broker; }
    public Groww getGroww() { return groww; }
    public Analysis getAnalysis() { return analysis; }

    /** JWT signing settings (app login). */
    public static class Security {
        private String jwtSecret;
        private long jwtExpiryMinutes = 10080;
        private String googleClientId;
        private String credentialAesKey;
        public String getCredentialAesKey() { return credentialAesKey; }
        public void setCredentialAesKey(String v) { this.credentialAesKey = v; }
        public String getGoogleClientId() { return googleClientId; }
        public void setGoogleClientId(String v) { this.googleClientId = v; }
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

}
