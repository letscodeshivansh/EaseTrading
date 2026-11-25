package com.easetrading.api.broker.groww;

import com.easetrading.api.broker.BrokerAdapter;
import com.easetrading.api.broker.BrokerModels.*;
import com.easetrading.api.broker.groww.GrowwRateLimiter.Category;
import com.easetrading.api.common.TtlCache;
import com.easetrading.api.instrument.Instrument;
import com.easetrading.api.marketdata.CandleDto;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * The real broker: Groww Trade API (https://groww.in/trade-api/docs/curl).
 * Active when easetrading.broker.provider = groww.
 *
 * READ-ONLY for now (phases G1–G4). Order placement is added in phase G5.
 */
@Component
@ConditionalOnProperty(name = "easetrading.broker.provider", havingValue = "groww")
public class GrowwAdapter implements BrokerAdapter {

    private static final ZoneId IST = ZoneId.of("Asia/Kolkata");
    private static final DateTimeFormatter TS = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").withZone(IST);
    /** Groww accepts at most 50 instruments per live-data call. */
    private static final int BATCH = 50;

    private final GrowwHttp http;
    private final GrowwTokenProvider tokens;

    private final TtlCache<BrokerProfile> profileCache = new TtlCache<>(Duration.ofMinutes(5));
    private final TtlCache<BrokerFunds> fundsCache = new TtlCache<>(Duration.ofSeconds(10));
    private final TtlCache<List<BrokerHolding>> holdingsCache = new TtlCache<>(Duration.ofSeconds(5));
    private final TtlCache<List<BrokerPosition>> positionsCache = new TtlCache<>(Duration.ofSeconds(5));

    public GrowwAdapter(GrowwHttp http, GrowwTokenProvider tokens) {
        this.http = http;
        this.tokens = tokens;
        System.out.println("[Broker] GROWW — real market data and portfolio (read-only).");
    }

    @Override
    public String source() { return "GROWW"; }

    @Override
    public BrokerStatus status() {
        try {
            BrokerProfile p = profile();
            return new BrokerStatus("GROWW", true, tokens.expiresAt(),
                    "Connected" + (p.clientId() != null ? " as " + mask(p.clientId()) : ""));
        } catch (Exception e) {
            return new BrokerStatus("GROWW", false, null, e.getMessage());
        }
    }

    @Override
    public void reconnect() {
        profileCache.clear();
        tokens.reconnect();
    }

    @Override
    public BrokerProfile profile() {
        return profileCache.get(() -> GrowwMapper.profile(
                http.get("/v1/user/detail", Map.of(), Category.OTHER)));
    }

    @Override
    public BrokerFunds funds() {
        return fundsCache.get(() -> GrowwMapper.funds(
                http.get("/v1/margins/detail/user", Map.of(), Category.NON_TRADING)));
    }

    @Override
    public List<BrokerHolding> holdings() {
        return holdingsCache.get(() -> GrowwMapper.holdings(
                http.get("/v1/holdings/user", Map.of(), Category.NON_TRADING)));
    }

    @Override
    public List<BrokerPosition> positions() {
        return positionsCache.get(() -> GrowwMapper.positions(
                http.get("/v1/positions/user", Map.of("segment", "CASH"), Category.NON_TRADING)));
    }

    @Override
    public Map<String, Double> ltp(List<Instrument> instruments) {
        Map<String, Double> out = new HashMap<>();
        for (List<Instrument> batch : batches(instruments)) {
            Map<String, String> keyToToken = keyMap(batch);
            var raw = GrowwMapper.ltp(http.get("/v1/live-data/ltp",
                    Map.of("segment", "CASH", "exchange_symbols", String.join(",", keyToToken.keySet())),
                    Category.LIVE_DATA));
            raw.forEach((key, price) -> {
                String token = keyToToken.get(key);
                if (token != null) out.put(token, price);
            });
        }
        return out;
    }

    @Override
    public Map<String, DayOhlc> ohlc(List<Instrument> instruments) {
        Map<String, DayOhlc> out = new HashMap<>();
        for (List<Instrument> batch : batches(instruments)) {
            Map<String, String> keyToToken = keyMap(batch);
            var raw = GrowwMapper.ohlc(http.get("/v1/live-data/ohlc",
                    Map.of("segment", "CASH", "exchange_symbols", String.join(",", keyToToken.keySet())),
                    Category.LIVE_DATA));
            raw.forEach((key, o) -> {
                String token = keyToToken.get(key);
                if (token != null) out.put(token, o);
            });
        }
        return out;
    }

    @Override
    public BrokerQuote quote(Instrument instrument) {
        return GrowwMapper.quote(http.get("/v1/live-data/quote", Map.of(
                "exchange", instrument.getExchange(),
                "segment", "CASH",
                "trading_symbol", instrument.getTradingSymbol()), Category.LIVE_DATA));
    }

    @Override
    public List<CandleDto> candles(Instrument instrument, String interval, Instant from, Instant to) {
        String growwInterval = growwInterval(interval);
        Duration maxChunk = maxRange(growwInterval);

        // Groww limits how much history one request may cover, so walk the range in chunks.
        List<CandleDto> out = new ArrayList<>();
        Instant chunkStart = from;
        while (chunkStart.isBefore(to)) {
            Instant chunkEnd = chunkStart.plus(maxChunk).isBefore(to) ? chunkStart.plus(maxChunk) : to;
            var payload = http.get("/v1/historical/candles", Map.of(
                    "exchange", instrument.getExchange(),
                    "segment", "CASH",
                    "groww_symbol", instrument.getGrowwSymbol(),
                    "start_time", TS.format(chunkStart),
                    "end_time", TS.format(chunkEnd),
                    "candle_interval", growwInterval), Category.OTHER);
            out.addAll(GrowwMapper.candles(payload, instrument.getToken(), interval));
            chunkStart = chunkEnd;
        }
        // De-duplicate bars on chunk boundaries and keep time order.
        Map<Instant, CandleDto> byTs = new TreeMap<>();
        out.forEach(c -> byTs.put(c.ts(), c));
        return new ArrayList<>(byTs.values());
    }

    // ------------------------------------------------------------------ helpers

    /** Our interval names -> Groww candle_interval values. */
    static String growwInterval(String interval) {
        return switch (interval) {
            case "1m" -> "1minute";
            case "3m" -> "3minute";
            case "5m" -> "5minute";
            case "15m" -> "15minute";
            case "30m" -> "30minute";
            case "1h" -> "1hour";
            case "1w" -> "1week";
            default -> "1day";
        };
    }

    /** Max history per request (Groww docs): 1–5 min = 30 days, 10–30 min = 90 days, others 180 days. */
    static Duration maxRange(String growwInterval) {
        return switch (growwInterval) {
            case "1minute", "2minute", "3minute", "5minute" -> Duration.ofDays(29);
            case "10minute", "15minute", "30minute" -> Duration.ofDays(89);
            default -> Duration.ofDays(179);
        };
    }

    /** "NSE_RELIANCE" -> token, the key format Groww uses for multi-instrument calls. */
    private static Map<String, String> keyMap(List<Instrument> batch) {
        Map<String, String> m = new LinkedHashMap<>();
        for (Instrument i : batch) m.put(i.getExchange() + "_" + i.getTradingSymbol(), i.getToken());
        return m;
    }

    private static List<List<Instrument>> batches(List<Instrument> all) {
        List<List<Instrument>> out = new ArrayList<>();
        for (int i = 0; i < all.size(); i += BATCH) out.add(all.subList(i, Math.min(all.size(), i + BATCH)));
        return out;
    }

    private static String mask(String id) {
        return id.length() <= 4 ? "****" : "****" + id.substring(id.length() - 4);
    }
}
