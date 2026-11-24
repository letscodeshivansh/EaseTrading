package com.easetrading.api.instrument;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Parses Groww's instrument master CSV
 * (https://growwapi-assets.groww.in/instruments/instrument.csv).
 *
 * Columns used: exchange, exchange_token, trading_symbol, groww_symbol, name,
 * instrument_type, segment, isin, lot_size, tick_size.
 *
 * We keep only NSE cash-market equities (exchange=NSE, segment=CASH,
 * instrument_type=EQ) — decision D1. The file also contains BSE, indices and
 * thousands of F&O contracts, which we skip.
 */
public final class InstrumentCsv {

    private InstrumentCsv() { }

    public static List<Instrument> parseNseEquities(BufferedReader reader) throws IOException {
        String headerLine = reader.readLine();
        if (headerLine == null) return List.of();
        Map<String, Integer> col = new HashMap<>();
        List<String> header = splitLine(headerLine);
        for (int i = 0; i < header.size(); i++) col.put(header.get(i).trim().toLowerCase(), i);

        List<Instrument> out = new ArrayList<>();
        String line;
        while ((line = reader.readLine()) != null) {
            if (line.isBlank()) continue;
            List<String> f = splitLine(line);
            if (!"NSE".equalsIgnoreCase(get(f, col, "exchange"))
                    || !"CASH".equalsIgnoreCase(get(f, col, "segment"))
                    || !"EQ".equalsIgnoreCase(get(f, col, "instrument_type"))) {
                continue;
            }
            String token = get(f, col, "exchange_token");
            String tradingSymbol = get(f, col, "trading_symbol");
            if (token.isBlank() || tradingSymbol.isBlank()) continue;

            String display = tradingSymbol.endsWith("-EQ")
                    ? tradingSymbol.substring(0, tradingSymbol.length() - 3) : tradingSymbol;
            String name = get(f, col, "name");
            String growwSymbol = get(f, col, "groww_symbol");

            out.add(new Instrument(token, display, name.isBlank() ? display : name, "NSE", "CASH")
                    .withDetails("EQ", tradingSymbol,
                            growwSymbol.isBlank() ? "NSE-" + display : growwSymbol,
                            blankToNull(get(f, col, "isin")),
                            parseInt(get(f, col, "lot_size")),
                            parseDouble(get(f, col, "tick_size"))));
        }
        return out;
    }

    /** Splits one CSV line, honouring double-quoted fields (names can contain commas). */
    static List<String> splitLine(String line) {
        List<String> out = new ArrayList<>();
        StringBuilder cur = new StringBuilder();
        boolean quoted = false;
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (c == '"') {
                if (quoted && i + 1 < line.length() && line.charAt(i + 1) == '"') { cur.append('"'); i++; }
                else quoted = !quoted;
            } else if (c == ',' && !quoted) {
                out.add(cur.toString());
                cur.setLength(0);
            } else {
                cur.append(c);
            }
        }
        out.add(cur.toString());
        return out;
    }

    private static String get(List<String> f, Map<String, Integer> col, String name) {
        Integer i = col.get(name);
        return (i == null || i >= f.size()) ? "" : f.get(i).trim();
    }

    private static String blankToNull(String s) { return s == null || s.isBlank() ? null : s; }

    private static Integer parseInt(String s) {
        try { return s.isBlank() ? null : (int) Double.parseDouble(s); } catch (NumberFormatException e) { return null; }
    }

    private static Double parseDouble(String s) {
        try { return s.isBlank() ? null : Double.parseDouble(s); } catch (NumberFormatException e) { return null; }
    }
}
