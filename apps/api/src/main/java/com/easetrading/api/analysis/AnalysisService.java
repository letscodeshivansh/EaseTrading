package com.easetrading.api.analysis;

import com.easetrading.api.common.ApiException;
import com.easetrading.api.fundamental.Fundamental;
import com.easetrading.api.fundamental.FundamentalService;
import com.easetrading.api.instrument.Instrument;
import com.easetrading.api.instrument.InstrumentRepository;
import com.easetrading.api.marketdata.CandleDto;
import com.easetrading.api.marketdata.CandleService;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Orchestrates a single stock analysis:
 *   gather candles + fundamentals  ->  call the Python analyst  ->  persist the verdict.
 *
 * The Python service does the indicator math and writes the memo (Claude on the Pro
 * plan, or the grounded fallback). This class just assembles inputs and records the
 * result.
 */
@Service
public class AnalysisService {

    private final InstrumentRepository instruments;
    private final CandleService candleService;
    private final FundamentalService fundamentalService;
    private final AnalysisClient analysisClient;
    private final AnalysisReportRepository reports;

    public AnalysisService(InstrumentRepository instruments, CandleService candleService,
                           FundamentalService fundamentalService, AnalysisClient analysisClient,
                           AnalysisReportRepository reports) {
        this.instruments = instruments;
        this.candleService = candleService;
        this.fundamentalService = fundamentalService;
        this.analysisClient = analysisClient;
        this.reports = reports;
    }

    public JsonNode analyze(String token, String strategy) {
        Instrument inst = instruments.findById(token)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Unknown instrument: " + token));

        // 1) Gather inputs.
        Instant to = Instant.now();
        Instant from = to.minus(250, ChronoUnit.DAYS);
        List<CandleDto> candles = candleService.getCandles(token, "1d", from, to);
        Map<String, Object> fundamentals = toSnakeCase(fundamentalService.getForToken(token));

        // 2) Ask the analyst.
        JsonNode verdict = analysisClient.analyze(inst.getSymbol(), strategy, candles, fundamentals);

        // 3) Persist for the audit trail.
        persist(token, strategy, verdict);

        return verdict;
    }

    /** The Python service expects snake_case fundamental keys. */
    private Map<String, Object> toSnakeCase(Fundamental f) {
        Map<String, Object> m = new HashMap<>();
        m.put("pe_ratio", f.getPeRatio());
        m.put("eps", f.getEps());
        m.put("roe_pct", f.getRoePct());
        m.put("profit_growth_pct", f.getProfitGrowthPct());
        m.put("fii_holding_pct", f.getFiiHoldingPct());
        m.put("dii_holding_pct", f.getDiiHoldingPct());
        m.put("promoter_pct", f.getPromoterPct());
        m.put("debt_to_equity", f.getDebtToEquity());
        return m;
    }

    private void persist(String token, String strategy, JsonNode v) {
        AnalysisReport r = new AnalysisReport(token, strategy);
        r.setRating(v.path("rating").asText("NEUTRAL"));
        r.setEntry(asDouble(v, "entry"));
        r.setStopLoss(asDouble(v, "stopLoss"));
        r.setTarget(asDouble(v, "target"));
        r.setRrRatio(asDouble(v, "rrRatio"));
        r.setConfidence(asDouble(v, "confidence"));
        r.setSource(v.path("source").asText("grounded"));
        r.setMemo(v.path("memo").asText(""));
        r.setSignalsJson(v.path("signals").toString());
        reports.save(r);
    }

    private Double asDouble(JsonNode node, String field) {
        JsonNode v = node.path(field);
        return v.isNumber() ? v.asDouble() : null;
    }
}
