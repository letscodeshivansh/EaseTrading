package com.easetrading.api.screener;

import com.easetrading.api.analysis.AnalysisClient;
import com.easetrading.api.instrument.Instrument;
import com.easetrading.api.instrument.InstrumentRepository;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Runs the fundamental screen.
 *   POST /api/screener/run    body: { "rules": [ ... ] }  (rules optional)
 *
 * The backend builds the universe (all known instruments) and delegates rule
 * evaluation to the Python analysis service. Passing no rules uses the defaults
 * (P/E<30, ROE>25%, EPS>0, FII/DII>5%, profit growth>0).
 */
@RestController
@RequestMapping("/api/screener")
public class ScreenerController {

    private final InstrumentRepository instruments;
    private final AnalysisClient analysis;

    public ScreenerController(InstrumentRepository instruments, AnalysisClient analysis) {
        this.instruments = instruments;
        this.analysis = analysis;
    }

    public record ScreenRequest(Object rules) {} // null rules => analysis uses defaults

    @PostMapping("/run")
    public JsonNode run(@RequestBody(required = false) ScreenRequest body) {
        // Build the universe from the instrument master.
        List<Map<String, String>> universe = instruments.findAll().stream()
                .map(this::toUniverseItem)
                .toList();

        Object rules = body != null ? body.rules() : null;
        return analysis.screen(universe, rules);
    }

    private Map<String, String> toUniverseItem(Instrument i) {
        return Map.of("symbol", i.getSymbol(), "exchange", i.getExchange(), "token", i.getToken());
    }
}
