package com.easetrading.api.instrument;

import com.easetrading.api.broker.BrokerAdapter;
import com.easetrading.api.broker.BrokerModels.BrokerQuote;
import com.easetrading.api.common.ApiException;
import com.easetrading.api.marketdata.MarketSubscription;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Stocks.
 *   GET /api/instruments/search?q=tata  -> up to 20 matching stocks
 *   GET /api/instruments/{token}        -> one stock + its live quote (price, day change, 52-week range…)
 */
@RestController
@RequestMapping("/api/instruments")
public class InstrumentController {

    private final InstrumentStore repo;
    private final BrokerAdapter broker;
    private final MarketSubscription subscription;

    public InstrumentController(InstrumentStore repo, BrokerAdapter broker, MarketSubscription subscription) {
        this.repo = repo;
        this.broker = broker;
        this.subscription = subscription;
    }

    /** Lightweight view returned to the browser (no internal fields). */
    public record InstrumentView(String token, String symbol, String name, String exchange) {}

    public record StockDetail(InstrumentView instrument, String isin, Integer lotSize, Double tickSize,
                              BrokerQuote quote, String quoteError) {}

    @GetMapping("/search")
    public List<InstrumentView> search(@RequestParam("q") String query) {
        if (query == null || query.isBlank()) return List.of();
        String q = query.trim();
        return repo.search(q, 20)
                .stream()
                .map(InstrumentController::view)
                .toList();
    }

    @GetMapping("/{token}")
    public StockDetail detail(@PathVariable String token) {
        Instrument i = repo.get(token)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Unknown instrument: " + token));
        subscription.add(i); // start streaming its price while someone looks at it

        BrokerQuote quote = null;
        String error = null;
        try {
            quote = broker.quote(i);
        } catch (Exception e) {
            error = e.getMessage();
        }
        return new StockDetail(view(i), i.getIsin(), i.getLotSize(), i.getTickSize(), quote, error);
    }

    private static InstrumentView view(Instrument i) {
        return new InstrumentView(i.getToken(), i.getSymbol(), i.getName(), i.getExchange());
    }
}
