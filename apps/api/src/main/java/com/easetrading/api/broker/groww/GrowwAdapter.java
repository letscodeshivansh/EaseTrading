package com.easetrading.api.broker.groww;

import com.easetrading.api.broker.BrokerAdapter;
import com.easetrading.api.broker.BrokerModels.*;
import com.easetrading.api.broker.groww.GrowwRateLimiter.Category;
import com.easetrading.api.config.AppProperties;
import com.easetrading.api.instrument.Instrument;
import com.easetrading.api.marketdata.CandleDto;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * Market data from Groww (prices, quotes, candles) — shared by every user.
 * Active when easetrading.broker.provider = groww.
 *
 * Uses ONE shared session built from the keys in .env (the app owner's account).
 * Each user's own account data (holdings, funds…) goes through their own session:
 * see GrowwAccountClient and AccountService.
 */
@Component
@ConditionalOnProperty(name = "easetrading.broker.provider", havingValue = "groww")
public class GrowwAdapter implements BrokerAdapter {

    private static final ZoneId IST = ZoneId.of("Asia/Kolkata");
    private static final DateTimeFormatter TS = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").withZone(IST);
    /** Groww accepts at most 50 instruments per live-data call. */
    private static final int BATCH = 50;

    private final GrowwHttp http;
    private final GrowwSession market;

    public GrowwAdapter(GrowwHttp http, AppProperties props) {
        this.http = http;
        this.market = http.newSession(GrowwCredentials.fromEnv(props.getGroww()));
        System.out.println("[Broker] GROWW — market data via the .env account; account data per user.");
    }

    @Override
    public String source() { return "GROWW"; }

    /** Retry the market-data login once, only if it had failed (e.g. key not approved yet). */
    @Override
    public void reconnect() {
        if (market.hasError()) {
            try {
                market.reconnect();
            } catch (GrowwException e) {
                // Prices stay unavailable; the stock page shows the reason.
            }
        }
    }

    @Override
    public Map<String, Double> ltp(List<Instrument> instruments) {
        Map<String, Double> out = new HashMap<>();
        for (List<Instrument> batch : batches(instruments)) {
            Map<String, String> keyToToken = keyMap(batch);
            var raw = GrowwMapper.ltp(http.get(market, "/v1/live-data/ltp",
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
            var raw = GrowwMapper.ohlc(http.get(market, "/v1/live-data/ohlc",
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
        return GrowwMapper.quote(http.get(market, "/v1/live-data/quote", Map.of(
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
            var payload = http.get(market, "/v1/historical/candles", Map.of(
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

    @Override
    public List<LocalDate> expiries(Instrument underlying) {
        LocalDate today = LocalDate.now(IST);
        TreeSet<LocalDate> all = new TreeSet<>();
        for (LocalDate month : List.of(today, today.plusMonths(1))) {
            all.addAll(GrowwMapper.expiries(http.get(market, "/v1/historical/expiries", Map.of(
                    "exchange", underlying.getExchange(),
                    "underlying_symbol", underlying.getTradingSymbol(),
                    "year", month.getYear(),
                    "month", month.getMonthValue()), Category.OTHER)));
        }
        return all.tailSet(today, true).stream().limit(6).toList();
    }

    @Override
    public OptionChainData optionChain(Instrument underlying, LocalDate expiry) {
        String path = "/v1/option-chain/exchange/" + underlying.getExchange()
                + "/underlying/" + underlying.getTradingSymbol();
        return GrowwMapper.optionChain(http.get(market, path,
                Map.of("expiry_date", expiry.toString()), Category.LIVE_DATA));
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

}
