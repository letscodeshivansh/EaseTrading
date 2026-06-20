package com.easetrading.api.fundamental;

import com.easetrading.api.analysis.AnalysisClient;
import com.easetrading.api.common.ApiException;
import com.easetrading.api.instrument.Instrument;
import com.easetrading.api.instrument.InstrumentRepository;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.time.LocalDate;

/**
 * Serves fundamentals with a "today's cache" policy:
 *   - If we already fetched this instrument today, return the stored row (fast).
 *   - Otherwise, ask the Python analysis service, store the result, and return it.
 *
 * This keeps external calls to roughly once per stock per day.
 */
@Service
public class FundamentalService {

    private final FundamentalRepository repo;
    private final InstrumentRepository instruments;
    private final AnalysisClient analysis;

    public FundamentalService(FundamentalRepository repo, InstrumentRepository instruments,
                              AnalysisClient analysis) {
        this.repo = repo;
        this.instruments = instruments;
        this.analysis = analysis;
    }

    public Fundamental getForToken(String token) {
        LocalDate today = LocalDate.now();
        return repo.findByTokenAndAsOf(token, today)
                .orElseGet(() -> fetchAndStore(token, today));
    }

    private Fundamental fetchAndStore(String token, LocalDate today) {
        Instrument inst = instruments.findById(token)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Unknown instrument: " + token));

        JsonNode data = analysis.fundamentals(inst.getSymbol(), inst.getExchange());

        Fundamental f = new Fundamental(token, today);
        f.setPeRatio(asDouble(data, "pe_ratio"));
        f.setEps(asDouble(data, "eps"));
        f.setRoePct(asDouble(data, "roe_pct"));
        f.setProfitGrowthPct(asDouble(data, "profit_growth_pct"));
        f.setFiiHoldingPct(asDouble(data, "fii_holding_pct"));
        f.setDiiHoldingPct(asDouble(data, "dii_holding_pct"));
        f.setPromoterPct(asDouble(data, "promoter_pct"));
        f.setDebtToEquity(asDouble(data, "debt_to_equity"));
        f.setSource(data.path("source").asText("unknown"));
        return repo.save(f);
    }

    /** Reads a numeric field, treating JSON null as Java null. */
    private Double asDouble(JsonNode node, String field) {
        JsonNode v = node.path(field);
        return v.isNumber() ? v.asDouble() : null;
    }
}
