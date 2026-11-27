package com.easetrading.api.broker.groww;

import com.easetrading.api.broker.BrokerModels.*;
import com.easetrading.api.marketdata.CandleDto;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Parses responses shaped like the samples in Groww's REST docs
 * (https://groww.in/trade-api/docs/curl). Goes through GrowwHttp.unwrap first, exactly
 * like real responses do.
 */
class GrowwMapperTest {

    private static JsonNode payload(String json) {
        return GrowwHttp.unwrap(200, json.getBytes(StandardCharsets.UTF_8));
    }

    @Test
    void failureEnvelopeBecomesGrowwExceptionWithCode() {
        String body = """
            {"status":"FAILURE","error":{"code":"GA005","message":"User not authorised","metadata":null}}""";
        GrowwException e = assertThrows(GrowwException.class,
                () -> GrowwHttp.unwrap(200, body.getBytes(StandardCharsets.UTF_8)));
        assertEquals("GA005", e.getCode());
        assertTrue(e.isAuthError());
    }

    @Test
    void http401WithoutEnvelopeIsAnAuthError() {
        GrowwException e = assertThrows(GrowwException.class,
                () -> GrowwHttp.unwrap(401, "{}".getBytes(StandardCharsets.UTF_8)));
        assertTrue(e.isAuthError());
    }

    @Test
    void unEnvelopedTokenResponseIsReturnedAsIs() {
        JsonNode p = GrowwHttp.unwrap(200,
                "{\"token\":\"abc\",\"expiry\":\"2024-07-01T12:34:56\",\"isActive\":true}".getBytes());
        assertEquals("abc", p.path("token").asText());
    }

    @Test
    void holdings() {
        List<BrokerHolding> h = GrowwMapper.holdings(payload("""
            {"status":"SUCCESS","payload":{"holdings":[
              {"isin":"INE002A01018","trading_symbol":"RELIANCE","quantity":10,"average_price":100,
               "pledge_quantity":0,"demat_locked_quantity":0,"groww_locked_quantity":0,
               "repledge_quantity":0,"t1_quantity":3,"demat_free_quantity":7}]}}"""));
        assertEquals(1, h.size());
        assertEquals("RELIANCE", h.get(0).tradingSymbol());
        assertEquals(10, h.get(0).quantity());
        assertEquals(100.0, h.get(0).averagePrice());
        assertEquals(3, h.get(0).t1Quantity());
    }

    @Test
    void funds() {
        BrokerFunds f = GrowwMapper.funds(payload("""
            {"status":"SUCCESS","payload":{"clear_cash":5000.5,"net_margin_used":120,
              "brokerage_and_charges":20,"equity_margin_details":{"cnc_balance_available":4800}}}"""));
        assertEquals(5000.5, f.availableCash());
        assertEquals(4800, f.cncAvailable());
    }

    @Test
    void ltpMap() {
        Map<String, Double> m = GrowwMapper.ltp(payload("""
            {"status":"SUCCESS","payload":{"NSE_RELIANCE":2334.2,"BSE_SENSEX":73243.4}}"""));
        assertEquals(2334.2, m.get("NSE_RELIANCE"));
        assertEquals(2, m.size());
    }

    @Test
    void ohlcStringIsParsed() {
        Map<String, DayOhlc> m = GrowwMapper.ohlc(payload("""
            {"status":"SUCCESS","payload":{"NSE_RELIANCE":"{open: 149.50,high: 150.50,low: 148.50,close: 149.50}"}}"""));
        DayOhlc o = m.get("NSE_RELIANCE");
        assertEquals(149.5, o.open());
        assertEquals(150.5, o.high());
        assertEquals(148.5, o.low());
        assertEquals(149.5, o.close());
    }

    @Test
    void quote() {
        BrokerQuote q = GrowwMapper.quote(payload("""
            {"status":"SUCCESS","payload":{"last_price":150.25,"day_change":1.25,"day_change_perc":0.84,
              "ohlc":"{open: 149.50,high: 150.50,low: 148.50,close: 149.00}","volume":12345,
              "week_52_high":180,"week_52_low":120,"market_cap":1000000,"last_trade_time":1700000000000}}"""));
        assertEquals(150.25, q.lastPrice());
        assertEquals(0.84, q.dayChangePct());
        assertEquals(149.0, q.close());
        assertEquals(12345L, q.volume());
        assertEquals(Instant.ofEpochMilli(1700000000000L), q.lastTradeTime());
    }

    @Test
    void candlesWithStringTimestampsAreReadAsIst() {
        List<CandleDto> c = GrowwMapper.candles(payload("""
            {"status":"SUCCESS","payload":{"candles":[
              ["2025-01-01T00:00:00",100,110,95,105,1000,null],
              ["2025-01-02 00:00:00",105,112,101,111,2000,null]],
              "interval_in_minutes":1440}}"""), "2885", "1d");
        assertEquals(2, c.size());
        // Midnight IST = 18:30 UTC the previous day.
        assertEquals(Instant.parse("2024-12-31T18:30:00Z"), c.get(0).ts());
        assertEquals(105.0, c.get(0).close());
        assertEquals(2000L, c.get(1).volume());
        assertEquals("2885", c.get(1).token());
    }

    @Test
    void candlesWithEpochSecondsAlsoWork() {
        List<CandleDto> c = GrowwMapper.candles(payload("""
            {"status":"SUCCESS","payload":{"candles":[[1735669800,100,110,95,105,1000]]}}"""), "2885", "1d");
        assertEquals(Instant.ofEpochSecond(1735669800), c.get(0).ts());
    }

    @Test
    void profile() {
        BrokerProfile p = GrowwMapper.profile(payload("""
            {"status":"SUCCESS","payload":{"vendor_user_id":"x","ucc":"ABC123","nse_enabled":true,
              "bse_enabled":false,"ddpi_enabled":false,"active_segments":["CASH","FNO"]}}"""));
        assertEquals("ABC123", p.clientId());
        assertTrue(p.nseEnabled());
        assertEquals(List.of("CASH", "FNO"), p.activeSegments());
    }

    @Test
    void optionChainAndExpiries() {
        OptionChainData c = GrowwMapper.optionChain(payload("""
            {"status":"SUCCESS","payload":{"underlying_ltp":24510.5,"strikes":{
              "24600":{"CE":{"trading_symbol":"NIFTY25D0224600CE","ltp":80.5,"open_interest":1000,"volume":50,
                             "greeks":{"delta":0.42,"gamma":0.001,"theta":-12.5,"vega":9.1,"rho":1.2,"iv":13.4}},
                       "PE":{"trading_symbol":"NIFTY25D0224600PE","ltp":160,"open_interest":700,"volume":40}},
              "24500":{"CE":{"trading_symbol":"NIFTY25D0224500CE","ltp":130,"open_interest":900,"volume":60},
                       "PE":{"trading_symbol":"NIFTY25D0224500PE","ltp":110,"open_interest":1200,"volume":70}}}}}"""));
        assertEquals(24510.5, c.spot());
        assertEquals(24500, c.strikes().get(0).strike());   // sorted ascending
        assertEquals(0.42, c.strikes().get(1).call().greeks().delta());
        assertEquals(1200, c.strikes().get(0).put().openInterest());

        assertEquals(List.of(java.time.LocalDate.of(2026, 12, 1), java.time.LocalDate.of(2026, 12, 8)),
                GrowwMapper.expiries(payload("{\"status\":\"SUCCESS\",\"payload\":{\"expiries\":[\"2026-12-01\",\"2026-12-08\"]}}")));
        assertEquals(1, GrowwMapper.expiries(payload("{\"status\":\"SUCCESS\",\"payload\":[\"2026-12-01\"]}")).size());
    }
}
