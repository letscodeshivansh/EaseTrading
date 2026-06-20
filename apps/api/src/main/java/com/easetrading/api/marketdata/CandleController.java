package com.easetrading.api.marketdata;

import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

/**
 * Historical candles + latest quote for charts.
 *   GET /api/candles/{token}?interval=1d&days=180
 *   GET /api/quotes/{token}                      (latest cached price)
 */
@RestController
@RequestMapping("/api")
public class CandleController {

    private final CandleService candleService;
    private final QuoteCache quoteCache;

    public CandleController(CandleService candleService, QuoteCache quoteCache) {
        this.candleService = candleService;
        this.quoteCache = quoteCache;
    }

    @GetMapping("/candles/{token}")
    public List<CandleDto> candles(
            @PathVariable String token,
            @RequestParam(defaultValue = "1d") String interval,
            @RequestParam(defaultValue = "180") int days) {

        Instant to = Instant.now();
        Instant from = to.minus(days, ChronoUnit.DAYS);
        return candleService.getCandles(token, interval, from, to);
    }

    /** Returns the latest live price as raw JSON ({token, ltp, ts}) or 204 if none yet. */
    @GetMapping("/quotes/{token}")
    public String quote(@PathVariable String token) {
        String latest = quoteCache.latest(token);
        return latest != null ? latest : "{}";
    }
}
