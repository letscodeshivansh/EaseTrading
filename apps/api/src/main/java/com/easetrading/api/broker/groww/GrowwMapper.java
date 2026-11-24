package com.easetrading.api.broker.groww;

import com.easetrading.api.broker.BrokerModels.*;
import com.easetrading.api.marketdata.CandleDto;
import com.fasterxml.jackson.databind.JsonNode;

import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Converts Groww JSON payloads into our broker-neutral models.
 *
 * Pure static functions — no HTTP — so each one is unit-tested against the sample
 * responses from Groww's docs (see GrowwMapperTest). Field names follow
 * https://groww.in/trade-api/docs/curl exactly.
 */
public final class GrowwMapper {

    private GrowwMapper() { }

    private static final ZoneId IST = ZoneId.of("Asia/Kolkata");
    private static final DateTimeFormatter CANDLE_TS = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final Pattern OHLC_PART = Pattern.compile("(open|high|low|close)\\s*:\\s*(-?[0-9.]+)");

    // GET /v1/user/detail
    public static BrokerProfile profile(JsonNode p) {
        List<String> segments = new ArrayList<>();
        p.path("active_segments").forEach(s -> segments.add(s.asText()));
        return new BrokerProfile("GROWW", p.path("ucc").asText(null),
                p.path("nse_enabled").asBoolean(false), p.path("bse_enabled").asBoolean(false), segments);
    }

    // GET /v1/margins/detail/user
    public static BrokerFunds funds(JsonNode p) {
        JsonNode eq = p.path("equity_margin_details");
        return new BrokerFunds(
                p.path("clear_cash").asDouble(0),
                p.path("net_margin_used").asDouble(0),
                eq.path("cnc_balance_available").asDouble(0),
                p.path("brokerage_and_charges").asDouble(0));
    }

    // GET /v1/holdings/user
    public static List<BrokerHolding> holdings(JsonNode p) {
        List<BrokerHolding> out = new ArrayList<>();
        for (JsonNode h : p.path("holdings")) {
            out.add(new BrokerHolding(
                    h.path("trading_symbol").asText(),
                    h.path("isin").asText(null),
                    (int) h.path("quantity").asDouble(0),
                    h.path("average_price").asDouble(0),
                    (int) h.path("t1_quantity").asDouble(0)));
        }
        return out;
    }

    // GET /v1/positions/user
    public static List<BrokerPosition> positions(JsonNode p) {
        List<BrokerPosition> out = new ArrayList<>();
        for (JsonNode x : p.path("positions")) {
            out.add(new BrokerPosition(
                    x.path("trading_symbol").asText(),
                    x.path("exchange").asText("NSE"),
                    x.path("product").asText(null),
                    (int) x.path("quantity").asDouble(0),
                    x.path("net_price").asDouble(0),
                    x.path("realised_pnl").asDouble(0)));
        }
        return out;
    }

    // GET /v1/live-data/ltp  ->  {"NSE_RELIANCE": 2334.2, ...}
    public static Map<String, Double> ltp(JsonNode p) {
        Map<String, Double> out = new HashMap<>();
        p.fields().forEachRemaining(e -> {
            JsonNode v = e.getValue();
            if (v.isNumber()) out.put(e.getKey(), v.asDouble());
            else if (v.isTextual()) {
                try { out.put(e.getKey(), Double.parseDouble(v.asText())); } catch (NumberFormatException ignored) { }
            }
        });
        return out;
    }

    // GET /v1/live-data/ohlc  ->  {"NSE_RELIANCE": "{open: 149.50,high: 150.50,low: 148.50,close: 149.50}"}
    public static Map<String, DayOhlc> ohlc(JsonNode p) {
        Map<String, DayOhlc> out = new HashMap<>();
        p.fields().forEachRemaining(e -> {
            DayOhlc o = parseOhlc(e.getValue());
            if (o != null) out.put(e.getKey(), o);
        });
        return out;
    }

    /** Groww sends OHLC as a non-JSON string; accept a real object too in case they fix it. */
    public static DayOhlc parseOhlc(JsonNode v) {
        if (v == null || v.isNull() || v.isMissingNode()) return null;
        if (v.isObject()) {
            return new DayOhlc(v.path("open").asDouble(0), v.path("high").asDouble(0),
                    v.path("low").asDouble(0), v.path("close").asDouble(0));
        }
        Map<String, Double> parts = new HashMap<>();
        Matcher m = OHLC_PART.matcher(v.asText(""));
        while (m.find()) parts.put(m.group(1), Double.parseDouble(m.group(2)));
        if (parts.isEmpty()) return null;
        return new DayOhlc(parts.getOrDefault("open", 0.0), parts.getOrDefault("high", 0.0),
                parts.getOrDefault("low", 0.0), parts.getOrDefault("close", 0.0));
    }

    // GET /v1/live-data/quote
    public static BrokerQuote quote(JsonNode p) {
        DayOhlc o = parseOhlc(p.get("ohlc"));
        Long tradeMs = num(p, "last_trade_time") == null ? null : p.path("last_trade_time").asLong();
        return new BrokerQuote(
                p.path("last_price").asDouble(0),
                num(p, "day_change"), num(p, "day_change_perc"),
                o == null ? null : o.open(), o == null ? null : o.high(),
                o == null ? null : o.low(), o == null ? null : o.close(),
                num(p, "volume") == null ? null : p.path("volume").asLong(),
                num(p, "week_52_high"), num(p, "week_52_low"),
                num(p, "market_cap"),
                num(p, "upper_circuit_limit"), num(p, "lower_circuit_limit"),
                tradeMs == null ? null : Instant.ofEpochMilli(tradeMs));
    }

    // GET /v1/historical/candles  ->  candles: [[ts, open, high, low, close, volume, oi], ...]
    public static List<CandleDto> candles(JsonNode p, String token, String interval) {
        List<CandleDto> out = new ArrayList<>();
        for (JsonNode row : p.path("candles")) {
            if (!row.isArray() || row.size() < 5) continue;
            Instant ts = parseCandleTime(row.get(0));
            if (ts == null) continue;
            out.add(new CandleDto(token, interval, ts,
                    row.get(1).asDouble(), row.get(2).asDouble(), row.get(3).asDouble(), row.get(4).asDouble(),
                    row.size() > 5 ? row.get(5).asLong(0) : 0));
        }
        return out;
    }

    /** Accepts "yyyy-MM-dd HH:mm:ss", "yyyy-MM-ddTHH:mm:ss" (assumed IST), ISO with offset, or epoch s/ms. */
    static Instant parseCandleTime(JsonNode v) {
        if (v == null || v.isNull()) return null;
        if (v.isNumber()) {
            long n = v.asLong();
            return n > 100_000_000_000L ? Instant.ofEpochMilli(n) : Instant.ofEpochSecond(n);
        }
        String s = v.asText().trim();
        try {
            return OffsetDateTime.parse(s).toInstant();
        } catch (Exception ignored) { }
        try {
            return LocalDateTime.parse(s.replace('T', ' '), CANDLE_TS).atZone(IST).toInstant();
        } catch (Exception ignored) { }
        try {
            return LocalDate.parse(s).atStartOfDay(IST).toInstant();
        } catch (Exception ignored) { }
        return null;
    }

    private static Double num(JsonNode p, String field) {
        JsonNode v = p.get(field);
        if (v == null || v.isNull()) return null;
        if (v.isNumber()) return v.asDouble();
        try { return Double.parseDouble(v.asText()); } catch (NumberFormatException e) { return null; }
    }
}
