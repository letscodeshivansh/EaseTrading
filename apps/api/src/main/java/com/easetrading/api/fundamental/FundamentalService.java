package com.easetrading.api.fundamental;

import com.easetrading.api.analysis.AnalysisClient;
import com.easetrading.api.common.ApiException;
import com.easetrading.api.instrument.Instrument;
import com.easetrading.api.instrument.InstrumentStore;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Serves fundamentals with a "today's copy" policy, IN MEMORY:
 *   - Already fetched this stock today? Return that copy (fast).
 *   - Otherwise ask the Python analysis service, remember it for today, return it.
 *
 * Keeps external calls to about once per stock per day without a database table.
 */
@Service
public class FundamentalService {

    private static final ZoneId IST = ZoneId.of("Asia/Kolkata");

    private final InstrumentStore instruments;
    private final AnalysisClient analysis;
    private final Map<String, Fundamental> today = new ConcurrentHashMap<>();

    public FundamentalService(InstrumentStore instruments, AnalysisClient analysis) {
        this.instruments = instruments;
        this.analysis = analysis;
    }

    public Fundamental getForToken(String token) {
        LocalDate date = LocalDate.now(IST);
        Fundamental cached = today.get(token);
        if (cached != null && date.equals(cached.getAsOf())) return cached;

        Fundamental fresh = fetch(token, date);
        today.put(token, fresh);
        return fresh;
    }

    private Fundamental fetch(String token, LocalDate date) {
        Instrument inst = instruments.get(token)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Unknown instrument: " + token));

        JsonNode data = analysis.fundamentals(inst.getSymbol(), inst.getExchange());

        Fundamental f = new Fundamental(token, date);
        f.setPeRatio(asDouble(data, "pe_ratio"));
        f.setEps(asDouble(data, "eps"));
        f.setRoePct(asDouble(data, "roe_pct"));
        f.setProfitGrowthPct(asDouble(data, "profit_growth_pct"));
        f.setFiiHoldingPct(asDouble(data, "fii_holding_pct"));
        f.setDiiHoldingPct(asDouble(data, "dii_holding_pct"));
        f.setPromoterPct(asDouble(data, "promoter_pct"));
        f.setDebtToEquity(asDouble(data, "debt_to_equity"));
        f.setSource(data.path("source").asText("unknown"));
        return f;
    }

    /** Reads a numeric field, treating JSON null as Java null. */
    private Double asDouble(JsonNode node, String field) {
        JsonNode v = node.path(field);
        return v.isNumber() ? v.asDouble() : null;
    }
}
