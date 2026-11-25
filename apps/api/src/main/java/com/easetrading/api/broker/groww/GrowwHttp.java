package com.easetrading.api.broker.groww;

import com.easetrading.api.broker.groww.GrowwRateLimiter.Category;
import com.easetrading.api.config.AppProperties;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriBuilder;

import java.io.IOException;
import java.net.URI;
import java.util.Map;
import java.util.function.Function;

/**
 * Low-level HTTP to Groww. Every Groww call in the app goes through here, which
 * gives us in ONE place:
 *   - the required headers (Bearer token, Accept, X-API-VERSION: 1.0)
 *   - rate limiting per category
 *   - unwrapping {"status":"SUCCESS","payload":...} / {"status":"FAILURE","error":...}
 *   - dropping a rejected token (the next call logs in once — no retries)
 *
 * Never logs tokens, keys or request bodies.
 */
@Component
@ConditionalOnProperty(name = "easetrading.broker.provider", havingValue = "groww")
public class GrowwHttp {

    private static final ObjectMapper JSON = new ObjectMapper();

    private final RestClient http;
    private final GrowwTokenProvider tokens;
    private final GrowwRateLimiter limiter;

    public GrowwHttp(AppProperties props, GrowwTokenProvider tokens, GrowwRateLimiter limiter) {
        this.http = RestClient.builder()
                .baseUrl(props.getGroww().getBaseUrl())
                .requestFactory(timeouts())
                .build();
        this.tokens = tokens;
        this.limiter = limiter;
    }

    /** GET with query params; returns the unwrapped payload. */
    public JsonNode get(String path, Map<String, ?> query, Category category) {
        return send(HttpMethod.GET, b -> {
            UriBuilder ub = b.path(path);
            query.forEach((k, v) -> { if (v != null) ub.queryParam(k, v); });
            return ub.build();
        }, null, category);
    }

    /** POST with a JSON body; returns the unwrapped payload. */
    public JsonNode post(String path, Object body, Category category) {
        return send(HttpMethod.POST, b -> b.path(path).build(), body, category);
    }

    private JsonNode send(HttpMethod method, Function<UriBuilder, URI> uri, Object body, Category category) {
        try {
            return once(method, uri, body, category, tokens.get());
        } catch (GrowwException e) {
            // Token rejected: drop it so the next request logs in once. No retry here.
            if (e.isAuthError()) tokens.invalidate();
            throw e;
        }
    }

    private JsonNode once(HttpMethod method, Function<UriBuilder, URI> uri, Object body,
                          Category category, String accessToken) {
        limiter.acquire(category);
        try {
            var req = http.method(method)
                    .uri(uri)
                    .header("Authorization", "Bearer " + accessToken)
                    .header("X-API-VERSION", "1.0")
                    .accept(MediaType.APPLICATION_JSON);
            if (body != null) req = req.contentType(MediaType.APPLICATION_JSON).body(body);
            return req.exchange((request, response) ->
                    unwrap(response.getStatusCode().value(), response.getBody().readAllBytes()), true);
        } catch (GrowwException e) {
            throw e;
        } catch (Exception e) {
            throw new GrowwException("NETWORK", "request failed: " + e.getMessage());
        }
    }

    /**
     * Turns a raw Groww HTTP response into the payload, or throws GrowwException.
     * Also accepts un-enveloped JSON (the token endpoint may reply without a wrapper).
     * Static so it can be unit-tested with the sample responses from the docs.
     */
    public static JsonNode unwrap(int httpStatus, byte[] body) {
        JsonNode root;
        try {
            root = (body == null || body.length == 0) ? JSON.createObjectNode() : JSON.readTree(body);
        } catch (IOException e) {
            throw new GrowwException("HTTP" + httpStatus, "non-JSON response");
        }

        String status = root.path("status").asText("");
        if ("FAILURE".equalsIgnoreCase(status) || (httpStatus >= 400 && root.has("error"))) {
            JsonNode err = root.path("error");
            String code = err.path("code").asText(httpStatus >= 400 ? "HTTP" + httpStatus : "UNKNOWN");
            throw new GrowwException(code, err.path("message").asText("request failed"));
        }
        if (httpStatus >= 400) {
            throw new GrowwException("HTTP" + httpStatus, root.path("message").asText("request failed"));
        }
        return root.has("payload") ? root.get("payload") : root;
    }

    /** Fail fast instead of hanging a request thread if Groww is slow. */
    static SimpleClientHttpRequestFactory timeouts() {
        SimpleClientHttpRequestFactory f = new SimpleClientHttpRequestFactory();
        f.setConnectTimeout(5_000);
        f.setReadTimeout(20_000);
        return f;
    }
}
