package com.easetrading.api.broker.mock;

import com.easetrading.api.broker.BrokerModels.OptionStrike;
import com.easetrading.api.instrument.Indices;
import org.junit.jupiter.api.Test;

import java.time.DayOfWeek;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

class MockOptionsTest {

    private final LocalDate today = LocalDate.of(2026, 11, 28); // a Saturday

    @Test
    void normalCdfIsAccurate() {
        assertEquals(0.5, MockOptions.cdf(0), 1e-7);
        assertEquals(0.841345, MockOptions.cdf(1), 1e-6);
        assertEquals(0.158655, MockOptions.cdf(-1), 1e-6);
    }

    @Test
    void niftyHasWeeklyTuesdayExpiriesAndBankNiftyMonthly() {
        var nifty = MockOptions.expiries(Indices.get("NIFTY").orElseThrow(), today);
        assertEquals(LocalDate.of(2026, 12, 1), nifty.get(0));
        assertTrue(nifty.stream().allMatch(d -> d.getDayOfWeek() == DayOfWeek.TUESDAY));
        var bank = MockOptions.expiries(Indices.get("BANKNIFTY").orElseThrow(), today);
        assertEquals(LocalDate.of(2026, 12, 29), bank.get(0)); // last Tuesday of December
    }

    @Test
    void chainIsPricedSensibly() {
        var idx = Indices.get("NIFTY").orElseThrow();
        var chain = MockOptions.chain(idx, 24510, today.plusDays(10), today);
        assertEquals(25, chain.strikes().size());
        OptionStrike atm = chain.strikes().stream().filter(r -> r.strike() == 24500).findFirst().orElseThrow();
        // At the money: call delta ~ +0.5, put delta ~ -0.5, both premiums positive.
        assertEquals(0.5, atm.call().greeks().delta(), 0.1);
        assertEquals(-0.5, atm.put().greeks().delta(), 0.1);
        assertTrue(atm.call().ltp() > 0 && atm.put().ltp() > 0);
        // Calls get cheaper as the strike rises; puts get dearer.
        var first = chain.strikes().get(0);
        var last = chain.strikes().get(chain.strikes().size() - 1);
        assertTrue(first.call().ltp() > last.call().ltp());
        assertTrue(first.put().ltp() < last.put().ltp());
        // Time decay: theta is negative for option buyers.
        assertTrue(atm.call().greeks().theta() < 0);
    }
}
