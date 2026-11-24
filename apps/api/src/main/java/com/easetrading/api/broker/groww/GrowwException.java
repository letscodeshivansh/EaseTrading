package com.easetrading.api.broker.groww;

/**
 * A failure reported by Groww ({"status":"FAILURE","error":{code,message}}) or a
 * transport problem talking to Groww.
 *
 * Groww error codes:
 *   GA000 internal error · GA001 bad request · GA003 unable to serve ·
 *   GA004 not found · GA005 not authorised · GA006 cannot process ·
 *   GA007 duplicate order reference id
 */
public class GrowwException extends RuntimeException {

    private final String code;

    public GrowwException(String code, String message) {
        super("Groww " + (code == null ? "" : code + " ") + message);
        this.code = code;
    }

    public String getCode() { return code; }

    /** True when the token is missing/expired/rejected — the caller should re-login once. */
    public boolean isAuthError() {
        return "GA005".equals(code) || "HTTP401".equals(code) || "HTTP403".equals(code);
    }
}
