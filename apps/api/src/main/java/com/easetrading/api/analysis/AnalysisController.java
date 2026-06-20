package com.easetrading.api.analysis;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.web.bind.annotation.*;

/**
 * Request a verdict for one instrument under a chosen strategy.
 *   POST /api/analysis/{token}   body: { "strategy": "CITADEL" }
 *
 * Returns the grounded verdict (rating, entry/stop/target, signals, memo).
 */
@RestController
@RequestMapping("/api/analysis")
public class AnalysisController {

    private final AnalysisService service;

    public AnalysisController(AnalysisService service) {
        this.service = service;
    }

    public record AnalyzeRequest(String strategy) {}

    @PostMapping("/{token}")
    public JsonNode analyze(@PathVariable String token,
                            @RequestBody(required = false) AnalyzeRequest body) {
        String strategy = (body != null && body.strategy() != null) ? body.strategy() : "BLACK_BOX";
        return service.analyze(token, strategy);
    }
}
