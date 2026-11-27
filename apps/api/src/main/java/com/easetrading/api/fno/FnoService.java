package com.easetrading.api.fno;

import com.easetrading.api.broker.BrokerAdapter;
import com.easetrading.api.broker.BrokerModels.OptionChainData;
import com.easetrading.api.broker.BrokerModels.OptionStrike;
import com.easetrading.api.common.ApiException;
import com.easetrading.api.common.TtlCache;
import com.easetrading.api.instrument.Indices;
import com.easetrading.api.instrument.Indices.IndexInfo;
import com.easetrading.api.instrument.Instrument;
import com.easetrading.api.instrument.InstrumentStore;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * F&O (index options): expiries and the option chain, plus the few numbers a new
 * trader actually needs, worked out for them:
 *   PCR (put-call ratio), max pain, strongest support (most put OI) and
 *   resistance (most call OI), the at-the-money strike, days to expiry.
 * Read-only — no orders here.
 */
@Service
public class FnoService {

    private static final ZoneId IST = ZoneId.of("Asia/Kolkata");

    public record Underlying(String token, String symbol, String name, String exchange, int lotSize, int strikeStep) {}

    public record ChainView(String underlying, String name, int lotSize, LocalDate expiry, long daysToExpiry,
                            double spot, double atmStrike, Double pcr, Double maxPain,
                            long totalCallOi, long totalPutOi, Double support, Double resistance,
                            String reading, List<OptionStrike> strikes) {}

    private final BrokerAdapter broker;
    private final InstrumentStore store;
    private final Map<String, TtlCache<List<LocalDate>>> expiryCache = new ConcurrentHashMap<>();
    private final Map<String, TtlCache<ChainView>> chainCache = new ConcurrentHashMap<>();

    public FnoService(BrokerAdapter broker, InstrumentStore store) {
        this.broker = broker;
        this.store = store;
    }

    public List<Underlying> underlyings() {
        return Indices.ALL.stream()
                .map(i -> new Underlying(i.token(), i.symbol(), i.name(), i.exchange(), i.lotSize(), i.strikeStep()))
                .toList();
    }

    public List<LocalDate> expiries(String underlying) {
        Instrument inst = instrument(underlying);
        return expiryCache.computeIfAbsent(inst.getToken(), k -> new TtlCache<>(Duration.ofHours(1)))
                .get(() -> broker.expiries(inst));
    }

    public ChainView chain(String underlying, LocalDate expiry) {
        IndexInfo idx = info(underlying);
        Instrument inst = instrument(underlying);
        String key = idx.token() + "|" + expiry;
        return chainCache.computeIfAbsent(key, k -> new TtlCache<>(Duration.ofSeconds(10)))
                .get(() -> summarise(idx, expiry, broker.optionChain(inst, expiry)));
    }

    static ChainView summarise(IndexInfo idx, LocalDate expiry, OptionChainData data) {
        List<OptionStrike> rows = data.strikes();
        double spot = data.spot();
        double atm = rows.stream().mapToDouble(OptionStrike::strike)
                .reduce((a, b) -> Math.abs(a - spot) <= Math.abs(b - spot) ? a : b).orElse(spot);

        long callOi = rows.stream().mapToLong(r -> r.call() == null ? 0 : r.call().openInterest()).sum();
        long putOi = rows.stream().mapToLong(r -> r.put() == null ? 0 : r.put().openInterest()).sum();
        Double pcr = callOi > 0 ? Math.round((double) putOi / callOi * 100.0) / 100.0 : null;

        Double resistance = rows.stream().filter(r -> r.call() != null)
                .max((a, b) -> Long.compare(a.call().openInterest(), b.call().openInterest()))
                .map(OptionStrike::strike).orElse(null);
        Double support = rows.stream().filter(r -> r.put() != null)
                .max((a, b) -> Long.compare(a.put().openInterest(), b.put().openInterest()))
                .map(OptionStrike::strike).orElse(null);

        long days = Math.max(0, expiry.toEpochDay() - LocalDate.now(IST).toEpochDay());
        return new ChainView(idx.token(), idx.name(), idx.lotSize(), expiry, days, spot, atm, pcr,
                maxPain(rows), callOi, putOi, support, resistance, reading(pcr), rows);
    }

    /** The expiry price at which option buyers, in total, would get the least payout. */
    static Double maxPain(List<OptionStrike> rows) {
        Double best = null;
        double lowest = Double.MAX_VALUE;
        for (OptionStrike candidate : rows) {
            double k = candidate.strike();
            double payout = 0;
            for (OptionStrike r : rows) {
                if (r.call() != null) payout += r.call().openInterest() * Math.max(0, k - r.strike());
                if (r.put() != null) payout += r.put().openInterest() * Math.max(0, r.strike() - k);
            }
            if (payout < lowest) {
                lowest = payout;
                best = k;
            }
        }
        return best;
    }

    /** Plain-English reading of the PCR. Educational, not advice. */
    static String reading(Double pcr) {
        if (pcr == null) return "Not enough open interest to read.";
        if (pcr >= 1.3) return "Many more puts than calls are open — traders are defending lower levels (often read as bullish).";
        if (pcr <= 0.7) return "Many more calls than puts are open — traders expect a ceiling on the upside (often read as bearish).";
        return "Calls and puts are fairly balanced — no strong signal either way.";
    }

    private IndexInfo info(String underlying) {
        return Indices.get(underlying).orElseThrow(() ->
                new ApiException(HttpStatus.BAD_REQUEST, "Options are available on NIFTY, BANKNIFTY, FINNIFTY and SENSEX"));
    }

    private Instrument instrument(String underlying) {
        IndexInfo idx = info(underlying);
        return store.get(idx.token()).orElseThrow(() ->
                new ApiException(HttpStatus.SERVICE_UNAVAILABLE, "Stock list is still loading — try again in a moment"));
    }
}
