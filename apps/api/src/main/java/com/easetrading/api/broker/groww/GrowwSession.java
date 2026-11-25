package com.easetrading.api.broker.groww;

import com.fasterxml.jackson.databind.JsonNode;
import dev.samstevens.totp.code.DefaultCodeGenerator;
import dev.samstevens.totp.time.SystemTimeProvider;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.*;
import java.util.HexFormat;
import java.util.Map;

/**
 * One Groww login session: a set of credentials + the access token they produce.
 *
 * There is one session per connected user (their own keys, see AccountService)
 * and one shared session for market data (the keys in .env, see GrowwAdapter).
 *
 * Groww tokens expire every day at 6:00 AM IST. Login modes:
 *   approval : API key + secret. We send checksum = SHA-256(secret + timestamp).
 *              Groww needs the key approved once a day on its API-keys page.
 *   totp     : API key + TOTP secret. We generate the 6-digit code ourselves.
 *   token    : a pasted daily access token (in `secret`).
 *
 * Simple rule — no automatic retries: log in once when a token is needed. If that
 * fails, keep the error and don't try again until reconnect() (the Reconnect button).
 * This also keeps us far below Groww's 150 logins/day limit.
 */
public class GrowwSession {

    static final ZoneId IST = ZoneId.of("Asia/Kolkata");

    private final GrowwCredentials creds;
    private final RestClient http;

    private String token;
    private Instant expiresAt;
    private GrowwException lastError;

    GrowwSession(GrowwCredentials creds, RestClient http) {
        this.creds = creds;
        this.http = http;
    }

    /** A valid token. Logs in once if needed; after a failed login, throws that error until reconnect(). */
    public synchronized String get() {
        if (token != null && Instant.now().isBefore(expiresAt)) return token;
        if (lastError != null) throw lastError;
        try {
            login();
            return token;
        } catch (GrowwException e) {
            lastError = e;
            System.err.println("[Groww] " + e.getMessage());
            throw e;
        }
    }

    /** Groww rejected the token: drop it. The next get() logs in once. */
    public synchronized void invalidate() {
        token = null;
        expiresAt = null;
    }

    /** "Reconnect" button: forget the last error and log in now. */
    public synchronized void reconnect() {
        invalidate();
        lastError = null;
        get();
    }

    public synchronized boolean hasError() { return lastError != null; }
    public synchronized Instant expiresAt() { return expiresAt; }

    // ------------------------------------------------------------------ login

    private void login() {
        String mode = creds.authMode() == null ? "approval" : creds.authMode().trim().toLowerCase();
        switch (mode) {
            case "token" -> {
                require(creds.secret(), "access token");
                token = creds.secret().trim();
                expiresAt = nextExpiry(Instant.now());
            }
            case "approval" -> {
                require(creds.apiKey(), "API key");
                require(creds.secret(), "API secret");
                String ts = String.valueOf(Instant.now().getEpochSecond());
                exchange(Map.of(
                        "key_type", "approval",
                        "checksum", checksum(creds.secret(), ts),
                        "timestamp", ts));
            }
            case "totp" -> {
                require(creds.apiKey(), "API key");
                require(creds.secret(), "TOTP secret");
                exchange(Map.of("key_type", "totp", "totp", totp(creds.secret())));
            }
            default -> throw new GrowwException("CONFIG", "unknown login mode '" + mode + "' (use approval | totp | token)");
        }
    }

    /** POST /v1/token/api/access with the API key, store the returned token. */
    private void exchange(Map<String, String> body) {
        JsonNode payload;
        try {
            payload = http.post()
                    .uri("/v1/token/api/access")
                    .header("Authorization", "Bearer " + creds.apiKey().trim())
                    .header("X-API-VERSION", "1.0")
                    .accept(MediaType.APPLICATION_JSON)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .exchange((req, res) ->
                            GrowwHttp.unwrap(res.getStatusCode().value(), res.getBody().readAllBytes()), true);
        } catch (GrowwException e) {
            throw new GrowwException(e.getCode(), "login failed: " + e.getMessage()
                    + " — approve the API key for today on Groww's API-keys page, then press Reconnect");
        } catch (Exception e) {
            throw new GrowwException("NETWORK", "login failed: " + e.getMessage());
        }

        String t = payload.path("token").asText(null);
        if (t == null || t.isBlank()) {
            throw new GrowwException("AUTH", "login response contained no token");
        }
        token = t;
        expiresAt = parseExpiry(payload.path("expiry").asText(null));
    }

    // ------------------------------------------------------------------ helpers (static for tests)


    /** SHA-256 hex of (secret + timestamp), as Groww's "approval" flow requires. */
    static String checksum(String secret, String timestamp) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(md.digest((secret + timestamp).getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    /** Next 6:00 AM IST after `now` — when every Groww token dies. */
    static Instant nextExpiry(Instant now) {
        ZonedDateTime ist = now.atZone(IST);
        ZonedDateTime six = ist.toLocalDate().atTime(6, 0).atZone(IST);
        if (!ist.isBefore(six)) six = six.plusDays(1);
        return six.toInstant();
    }

    /** Groww's "expiry" is an ISO local time (assumed IST). Never later than the 6 AM cut-off. */
    static Instant parseExpiry(String raw) {
        Instant cutoff = nextExpiry(Instant.now());
        if (raw == null || raw.isBlank()) return cutoff;
        try {
            Instant parsed;
            try {
                parsed = OffsetDateTime.parse(raw).toInstant();
            } catch (Exception notOffset) {
                parsed = LocalDateTime.parse(raw).atZone(IST).toInstant();
            }
            return parsed.isBefore(cutoff) ? parsed : cutoff;
        } catch (Exception e) {
            return cutoff;
        }
    }

    private static String totp(String base32Secret) {
        try {
            long bucket = Math.floorDiv(new SystemTimeProvider().getTime(), 30);
            return new DefaultCodeGenerator().generate(base32Secret.trim(), bucket);
        } catch (Exception e) {
            throw new GrowwException("CONFIG", "could not generate a TOTP code — check the TOTP secret");
        }
    }

    private static void require(String value, String what) {
        if (value == null || value.isBlank()) {
            throw new GrowwException("CONFIG", "Groww " + what + " is missing");
        }
    }
}
