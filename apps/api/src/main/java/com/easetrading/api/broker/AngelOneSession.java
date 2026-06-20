package com.easetrading.api.broker;

/**
 * Holds the tokens returned by Angel One after a successful login.
 *  - jwtToken    : used to authorise REST calls (orders, candles, quotes)
 *  - feedToken   : used to authorise the live market-data WebSocket
 *  - refreshToken: used to obtain a fresh jwtToken when it expires
 */
public record AngelOneSession(String jwtToken, String feedToken, String refreshToken) {
    public boolean isValid() {
        return jwtToken != null && !jwtToken.isBlank();
    }
}
