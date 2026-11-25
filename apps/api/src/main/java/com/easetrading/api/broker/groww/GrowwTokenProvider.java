package com.easetrading.api.broker.groww;

import com.easetrading.api.config.AppProperties;
import com.fasterxml.jackson.databind.JsonNode;
import dev.samstevens.totp.code.DefaultCodeGenerator;
import dev.samstevens.totp.time.SystemTimeProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.*;
import java.util.HexFormat;
import java.util.Map;

/**
 * Gets and caches the Groww access token.
 *
 * Groww tokens expire every day at 6:00 AM IST. Three ways to get one
 * (GROWW_AUTH_MODE):
 *   token    : you paste a daily access token into GROWW_ACCESS_TOKEN
 *   approval : API key + secret. We send checksum = SHA-256(secret + timestamp).
 *              Groww needs the key approved once a day on its API-keys page.
 *   totp     : API key + TOTP secret. We generate the 6-digit code ourselves.
 *
 * Simple rule — no automatic retries: log in once when a token is needed. If that
 * fails (e.g. the key isn't approved today), keep the error, show it on the
 * dashboard, and don't try again until you press Reconnect (reconnect()).
 * This also keeps us far below Groww's 150 logins/day limit.
 */
@Component
@ConditionalOnProperty(name = "easetrading.broker.provider", havingValue = "groww")
public class GrowwTokenProvider {

    static final ZoneId IST = ZoneId.of("Asia/Kolkata");

    private final AppProperties.Groww cfg;
    private final RestClient http;

    private String token;
    private Instant expiresAt;
    private GrowwException lastError;

    public GrowwTokenProvider(AppProperties props) {
        this.cfg = props.getGroww();
        this.http = RestClient.builder()
                .baseUrl(cfg.getBaseUrl())
                .requestFactory(GrowwHttp.timeouts())
                .build();
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

    public synchronized Instant expiresAt() { return expiresAt; }
    public synchronized String lastErrorMessage() { return lastError == null ? null : lastError.getMessage(); }

    // ------------------------------------------------------------------ login

    private void login() {
        String mode = cfg.getAuthMode() == null ? "approval" : cfg.getAuthMode().trim().toLowerCase();
        switch (mode) {
            case "token" -> {
                require(cfg.getAccessToken(), "GROWW_ACCESS_TOKEN");
                token = cfg.getAccessToken().trim();
                expiresAt = nextExpiry(Instant.now());
            }
            case "approval" -> {
                require(cfg.getApiKey(), "GROWW_API_KEY");
                require(cfg.getApiSecret(), "GROWW_API_SECRET");
                String ts = String.valueOf(Instant.now().getEpochSecond());
                exchange(Map.of(
                        "key_type", "approval",
                        "checksum", checksum(cfg.getApiSecret(), ts),
                        "timestamp", ts));
            }
            case "totp" -> {
                require(cfg.getApiKey(), "GROWW_API_KEY");
                require(cfg.getTotpSecret(), "GROWW_TOTP_SECRET");
                exchange(Map.of("key_type", "totp", "totp", totp(cfg.getTotpSecret())));
            }
            default -> throw new GrowwException("CONFIG",
                    "unknown GROWW_AUTH_MODE '" + mode + "' (use token | approval | totp)");
        }
    }

    /** POST /v1/token/api/access with the API key, store the returned token. */
    private void exchange(Map<String, String> body) {
        JsonNode payload;
        try {
            payload = http.post()
                    .uri("/v1/token/api/access")
                    .header("Authorization", "Bearer " + cfg.getApiKey().trim())
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
            throw new GrowwException("CONFIG", "could not generate TOTP — check GROWW_TOTP_SECRET");
        }
    }

    private static void require(String value, String envName) {
        if (value == null || value.isBlank()) {
            throw new GrowwException("CONFIG", envName + " is not set in .env");
        }
    }
}
