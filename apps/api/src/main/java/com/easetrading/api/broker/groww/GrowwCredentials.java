package com.easetrading.api.broker.groww;

import com.easetrading.api.config.AppProperties;

/**
 * What a Groww login needs. `secret` is the API secret (approval mode), the TOTP
 * secret (totp mode) or the daily access token (token mode).
 */
public record GrowwCredentials(String authMode, String apiKey, String secret) {

    /** The shared market-data account from .env (GROWW_*). */
    public static GrowwCredentials fromEnv(AppProperties.Groww g) {
        String mode = g.getAuthMode() == null ? "approval" : g.getAuthMode().trim().toLowerCase();
        String secret = switch (mode) {
            case "totp" -> g.getTotpSecret();
            case "token" -> g.getAccessToken();
            default -> g.getApiSecret();
        };
        return new GrowwCredentials(mode, g.getApiKey(), secret);
    }

    /** Never print keys. */
    @Override
    public String toString() {
        return "GrowwCredentials[" + authMode + "]";
    }
}
