package com.easetrading.api.analysis;

import com.easetrading.api.config.AppProperties;
import com.easetrading.api.marketdata.CandleDto;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;

/**
 * HTTP client to the Python analysis service. This is the ONLY class in the backend
 * that knows the analysis service's URL and request shapes — everything else calls
 * these typed methods. If the Python contract changes, we update it here only.
 */
@Component
public class AnalysisClient {

    private final RestClient http;

    public AnalysisClient(AppProperties props) {
        this.http = RestClient.builder()
                .baseUrl(props.getAnalysis().getServiceUrl())
                .build();
    }

    /** Compute technical indicators for one instrument from its candles. */
    public JsonNode indicators(String token, String interval, List<CandleDto> candles) {
        return http.post()
                .uri("/indicators")
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("token", token, "interval", interval, "candles", candles))
                .retrieve()
                .body(JsonNode.class);
    }

    /** Fetch fundamentals for one symbol. */
    public JsonNode fundamentals(String symbol, String exchange) {
        return http.get()
                .uri(uri -> uri.path("/fundamentals/{symbol}").queryParam("exchange", exchange).build(symbol))
                .retrieve()
                .body(JsonNode.class);
    }

    /** Ask the analysis service for a grounded verdict (Claude or fallback). */
    public JsonNode analyze(String symbol, String strategy,
                            List<CandleDto> candles, Map<String, Object> fundamentals) {
        return http.post()
                .uri("/analyze")
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of(
                        "symbol", symbol,
                        "strategy", strategy,
                        "candles", candles,
                        "fundamentals", fundamentals))
                .retrieve()
                .body(JsonNode.class);
    }

    /** Run the fundamental screen over a universe of instruments. */
    public JsonNode screen(List<Map<String, String>> universe, Object rules) {
        return http.post()
                .uri("/screen")
                .contentType(MediaType.APPLICATION_JSON)
                .body(rules == null
                        ? Map.of("universe", universe)
                        : Map.of("universe", universe, "rules", rules))
                .retrieve()
                .body(JsonNode.class);
    }
}
