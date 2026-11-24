package com.easetrading.api.instrument;

import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.atomic.AtomicReference;

/**
 * The stock list (symbol master), kept IN MEMORY.
 *
 * It's small (≈4,300 NSE stocks), comes from the broker's daily file, and is rebuilt
 * at every startup by {@link InstrumentSync} — so it doesn't need a database table.
 * Lookups are plain map reads; the whole list is swapped atomically on refresh, so
 * readers never see a half-loaded list.
 */
@Component
public class InstrumentStore {

    /** Immutable snapshot of the list plus the lookup indexes built from it. */
    private record Snapshot(List<Instrument> all,
                            Map<String, Instrument> byToken,
                            Map<String, Instrument> byTradingSymbol,
                            Map<String, Instrument> bySymbol,
                            Map<String, Instrument> byIsin) {

        static Snapshot of(Collection<Instrument> list) {
            Map<String, Instrument> byToken = new LinkedHashMap<>();
            Map<String, Instrument> byTs = new HashMap<>();
            Map<String, Instrument> bySym = new HashMap<>();
            Map<String, Instrument> byIsin = new HashMap<>();
            for (Instrument i : list) {
                byToken.put(i.getToken(), i);
                byTs.putIfAbsent(key(i.getExchange(), i.getTradingSymbol()), i);
                bySym.putIfAbsent(key(i.getExchange(), i.getSymbol()), i);
                if (i.getIsin() != null) byIsin.putIfAbsent(i.getIsin(), i);
            }
            return new Snapshot(List.copyOf(byToken.values()), byToken, byTs, bySym, byIsin);
        }
    }

    private final AtomicReference<Snapshot> data = new AtomicReference<>(Snapshot.of(List.of()));

    /** Replace the whole list (used by the daily sync). */
    public void replaceAll(Collection<Instrument> instruments) {
        data.set(Snapshot.of(instruments));
    }

    public int count() { return data.get().all().size(); }

    public List<Instrument> all() { return data.get().all(); }

    public Optional<Instrument> get(String token) {
        return token == null ? Optional.empty() : Optional.ofNullable(data.get().byToken().get(token));
    }

    public boolean exists(String token) { return get(token).isPresent(); }

    public Optional<Instrument> byTradingSymbol(String tradingSymbol, String exchange) {
        return Optional.ofNullable(data.get().byTradingSymbol().get(key(exchange, tradingSymbol)));
    }

    public Optional<Instrument> bySymbol(String symbol, String exchange) {
        return Optional.ofNullable(data.get().bySymbol().get(key(exchange, symbol)));
    }

    public Optional<Instrument> byIsin(String isin) {
        return isin == null ? Optional.empty() : Optional.ofNullable(data.get().byIsin().get(isin));
    }

    /**
     * Search box: exact symbol first, then symbols starting with the query, then names
     * containing it. Up to `limit` results.
     */
    public List<Instrument> search(String query, int limit) {
        String q = query == null ? "" : query.trim().toUpperCase(Locale.ROOT);
        if (q.isEmpty()) return List.of();
        List<Instrument> exact = new ArrayList<>(), prefix = new ArrayList<>(), name = new ArrayList<>();
        for (Instrument i : data.get().all()) {
            String sym = i.getSymbol().toUpperCase(Locale.ROOT);
            if (sym.equals(q)) exact.add(i);
            else if (sym.startsWith(q)) prefix.add(i);
            else if (i.getName() != null && i.getName().toUpperCase(Locale.ROOT).contains(q)) name.add(i);
        }
        prefix.sort(Comparator.comparing(Instrument::getSymbol));
        name.sort(Comparator.comparing(Instrument::getSymbol));
        List<Instrument> out = new ArrayList<>(exact);
        out.addAll(prefix);
        out.addAll(name);
        return out.size() > limit ? out.subList(0, limit) : out;
    }

    private static String key(String exchange, String symbol) {
        return (exchange == null ? "NSE" : exchange) + ":" + (symbol == null ? "" : symbol.toUpperCase(Locale.ROOT));
    }
}
