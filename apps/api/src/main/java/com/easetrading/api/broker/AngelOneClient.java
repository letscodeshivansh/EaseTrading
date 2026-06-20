package com.easetrading.api.broker;

import com.easetrading.api.common.ApiException;
import com.easetrading.api.config.AppProperties;
import com.easetrading.api.marketdata.CandleDto;
import com.fasterxml.jackson.databind.JsonNode;
import dev.samstevens.totp.code.DefaultCodeGenerator;
import dev.samstevens.totp.time.SystemTimeProvider;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Concrete Angel One SmartAPI client (REST).
 *
 * Implements login (with auto-generated TOTP), historical candles, and last-traded
 * price. Live tick-by-tick streaming over the SmartAPI WebSocket can replace the
 * REST price polling later; for Prompt 1, REST polling is simpler and robust.
 *
 * NOTE: every SmartAPI request needs a set of "X-*" headers. We centralise them in
 * commonHeaders() so each call stays readable.
 */
@Component
public class AngelOneClient implements AngelOneAdapter {

    private static final DateTimeFormatter CANDLE_FMT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm").withZone(ZoneId.of("Asia/Kolkata"));

    private final AppProperties props;
    private final RestClient http;

    public AngelOneClient(AppProperties props) {
        this.props = props;
        this.http = RestClient.builder()
                .baseUrl(props.getAngelone().getBaseUrl())
                .build();
    }

    @Override
    public AngelOneSession login() {
        var cfg = props.getAngelone();
        if (cfg.getApiKey() == null || cfg.getApiKey().isBlank()) {
            throw new ApiException(org.springframework.http.HttpStatus.BAD_REQUEST,
                    "Angel One API key not configured. Set MARKET_FEED_MODE=mock for dev, " +
                    "or provide credentials for live mode.");
        }

        // SmartAPI requires a fresh time-based one-time password (TOTP) at login.
        String totp = generateTotp(cfg.getTotpSecret());

        JsonNode resp = http.post()
                .uri("/rest/auth/angelbroking/user/v1/loginByPassword")
                .headers(h -> commonHeaders(h, cfg.getApiKey()))
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of(
                        "clientcode", cfg.getClientId(),
                        "password", cfg.getPassword(),
                        "totp", totp))
                .retrieve()
                .body(JsonNode.class);

        JsonNode data = requireData(resp, "login");
        return new AngelOneSession(
                data.path("jwtToken").asText(),
                data.path("feedToken").asText(),
                data.path("refreshToken").asText());
    }

    @Override
    public List<CandleDto> getCandles(AngelOneSession session, String exchange, String token,
                                      String interval, Instant from, Instant to) {
        JsonNode resp = http.post()
                .uri("/rest/secure/angelbroking/historical/v1/getCandleData")
                .headers(h -> {
                    commonHeaders(h, props.getAngelone().getApiKey());
                    h.setBearerAuth(session.jwtToken());
                })
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of(
                        "exchange", exchange,
                        "symboltoken", token,
                        "interval", interval,
                        "fromdate", CANDLE_FMT.format(from),
                        "todate", CANDLE_FMT.format(to)))
                .retrieve()
                .body(JsonNode.class);

        JsonNode data = requireData(resp, "getCandleData");

        // Angel One returns each candle as [timestamp, open, high, low, close, volume].
        List<CandleDto> candles = new ArrayList<>();
        for (JsonNode row : data) {
            candles.add(new CandleDto(
                    token, interval,
                    OffsetDateTime.parse(row.get(0).asText()).toInstant(),
                    row.get(1).asDouble(),
                    row.get(2).asDouble(),
                    row.get(3).asDouble(),
                    row.get(4).asDouble(),
                    row.get(5).asLong()));
        }
        return candles;
    }

    @Override
    public double getLastTradedPrice(AngelOneSession session, String exchange, String symbol, String token) {
        JsonNode resp = http.post()
                .uri("/rest/secure/angelbroking/order/v1/getLtpData")
                .headers(h -> {
                    commonHeaders(h, props.getAngelone().getApiKey());
                    h.setBearerAuth(session.jwtToken());
                })
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of(
                        "exchange", exchange,
                        "tradingsymbol", symbol,
                        "symboltoken", token))
                .retrieve()
                .body(JsonNode.class);

        return requireData(resp, "getLtpData").path("ltp").asDouble();
    }

    @Override
    public String placeOrder(AngelOneSession session, String exchange, String symbol, String token,
                             String side, String type, int qty, double price) {
        // Build the SmartAPI order payload. DELIVERY product = normal equity holding.
        Map<String, Object> payload = new java.util.HashMap<>();
        payload.put("variety", "NORMAL");
        payload.put("tradingsymbol", symbol);
        payload.put("symboltoken", token);
        payload.put("transactiontype", side);          // BUY | SELL
        payload.put("exchange", exchange);
        payload.put("ordertype", type);                // MARKET | LIMIT
        payload.put("producttype", "DELIVERY");
        payload.put("duration", "DAY");
        payload.put("quantity", String.valueOf(qty));
        payload.put("price", type.equals("LIMIT") ? String.valueOf(price) : "0");

        JsonNode resp = http.post()
                .uri("/rest/secure/angelbroking/order/v1/placeOrder")
                .headers(h -> {
                    commonHeaders(h, props.getAngelone().getApiKey());
                    h.setBearerAuth(session.jwtToken());
                })
                .contentType(MediaType.APPLICATION_JSON)
                .body(payload)
                .retrieve()
                .body(JsonNode.class);

        return requireData(resp, "placeOrder").path("orderid").asText();
    }

    // ---------- helpers ----------

    /** Headers every SmartAPI request needs. The IP/MAC values can be placeholders. */
    private void commonHeaders(org.springframework.http.HttpHeaders h, String apiKey) {
        h.set("X-UserType", "USER");
        h.set("X-SourceID", "WEB");
        h.set("X-ClientLocalIP", "127.0.0.1");
        h.set("X-ClientPublicIP", "127.0.0.1");
        h.set("X-MACAddress", "00:00:00:00:00:00");
        h.set("X-PrivateKey", apiKey);
        h.set("Accept", "application/json");
    }

    /** Generates the current 6-digit TOTP from the shared secret. */
    private String generateTotp(String secret) {
        try {
            long bucket = Math.floorDiv(new SystemTimeProvider().getTime(), 30);
            return new DefaultCodeGenerator().generate(secret, bucket);
        } catch (Exception e) {
            throw new ApiException(org.springframework.http.HttpStatus.BAD_REQUEST,
                    "Could not generate TOTP — check ANGEL_ONE_TOTP_SECRET");
        }
    }

    /** Validates the SmartAPI envelope ({status, message, data}) and returns "data". */
    private JsonNode requireData(JsonNode resp, String op) {
        if (resp == null || !resp.path("status").asBoolean(false)) {
            String msg = resp == null ? "no response" : resp.path("message").asText("unknown error");
            throw new ApiException(org.springframework.http.HttpStatus.BAD_GATEWAY,
                    "Angel One " + op + " failed: " + msg);
        }
        return resp.path("data");
    }
}
