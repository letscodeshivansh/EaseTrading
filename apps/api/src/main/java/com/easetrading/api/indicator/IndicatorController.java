package com.easetrading.api.indicator;

import com.easetrading.api.analysis.AnalysisClient;
import com.easetrading.api.marketdata.CandleDto;
import com.easetrading.api.marketdata.CandleService;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

/**
 * Technical indicators for one instrument, used to draw overlays (moving averages,
 * Bollinger, Fibonacci) on the chart and to feed the strategy layer later.
 *
 *   GET /api/indicators/{token}?interval=1d&days=250
 *
 * The backend gathers candles and delegates the math to the Python analysis service,
 * so the formulas live in exactly one place.
 */
@RestController
@RequestMapping("/api/indicators")
public class IndicatorController {

    private final CandleService candleService;
    private final AnalysisClient analysis;

    public IndicatorController(CandleService candleService, AnalysisClient analysis) {
        this.candleService = candleService;
        this.analysis = analysis;
    }

    @GetMapping("/{token}")
    public JsonNode indicators(
            @PathVariable String token,
            @RequestParam(defaultValue = "1d") String interval,
            @RequestParam(defaultValue = "250") int days) {

        Instant to = Instant.now();
        Instant from = to.minus(days, ChronoUnit.DAYS);
        List<CandleDto> candles = candleService.getCandles(token, interval, from, to);
        return analysis.indicators(token, interval, candles);
    }
}
