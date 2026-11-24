package com.easetrading.api.instrument;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class InstrumentStoreTest {

    private InstrumentStore store() {
        InstrumentStore s = new InstrumentStore();
        s.replaceAll(List.of(
                new Instrument("1", "TATAMOTORS", "Tata Motors Ltd", "NSE", "CASH"),
                new Instrument("2", "TCS", "Tata Consultancy Services", "NSE", "CASH"),
                new Instrument("3", "TATA", "Tata Investment Corp", "NSE", "CASH")
                        .withDetails("EQ", "TATA-EQ", "NSE-TATA", "INE000X01010", 1, 0.05),
                new Instrument("4", "INFY", "Infosys Ltd", "NSE", "CASH")));
        return s;
    }

    @Test
    void searchRanksExactThenPrefixThenName() {
        var r = store().search("tata", 20).stream().map(Instrument::getSymbol).toList();
        // exact "TATA" first, then symbols starting with TATA, then names containing it.
        assertEquals(List.of("TATA", "TATAMOTORS", "TCS"), r);
    }

    @Test
    void lookupsByTokenTradingSymbolAndIsin() {
        InstrumentStore s = store();
        assertEquals("INFY", s.get("4").orElseThrow().getSymbol());
        assertEquals("3", s.byTradingSymbol("tata-eq", "NSE").orElseThrow().getToken());
        assertEquals("3", s.byIsin("INE000X01010").orElseThrow().getToken());
        assertTrue(s.get("999").isEmpty());
        assertTrue(store().search("  ", 20).isEmpty());
    }

    @Test
    void replaceAllSwapsTheWholeList() {
        InstrumentStore s = store();
        s.replaceAll(List.of(new Instrument("9", "SBIN", "State Bank", "NSE", "CASH")));
        assertEquals(1, s.count());
        assertFalse(s.exists("1"));
    }
}
