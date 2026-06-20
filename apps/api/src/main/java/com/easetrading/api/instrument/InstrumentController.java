package com.easetrading.api.instrument;

import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Instrument search for the symbol picker / watchlist.
 *   GET /api/instruments/search?q=rel  -> matching instruments
 */
@RestController
@RequestMapping("/api/instruments")
public class InstrumentController {

    private final InstrumentRepository repo;

    public InstrumentController(InstrumentRepository repo) {
        this.repo = repo;
    }

    /** Lightweight view returned to the browser (no internal fields). */
    public record InstrumentView(String token, String symbol, String name, String exchange) {}

    @GetMapping("/search")
    public List<InstrumentView> search(@RequestParam("q") String query) {
        if (query == null || query.isBlank()) return List.of();
        return repo.findTop20BySymbolStartingWithIgnoreCaseOrNameContainingIgnoreCase(query, query)
                .stream()
                .map(i -> new InstrumentView(i.getToken(), i.getSymbol(), i.getName(), i.getExchange()))
                .toList();
    }
}
