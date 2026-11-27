package com.easetrading.api.fno;

import com.easetrading.api.broker.BrokerModels.*;
import com.easetrading.api.instrument.Indices;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class FnoServiceTest {

    private static OptionLeg leg(long oi) {
        return new OptionLeg("X", 10, oi, 0, null);
    }

    /** 3 strikes: heavy put OI at 100 (support), heavy call OI at 120 (resistance). */
    private static final List<OptionStrike> CHAIN = List.of(
            new OptionStrike(100, leg(100), leg(900)),
            new OptionStrike(110, leg(300), leg(300)),
            new OptionStrike(120, leg(900), leg(100)));

    @Test
    void summaryNumbers() {
        var idx = Indices.get("NIFTY").orElseThrow();
        var view = FnoService.summarise(idx, LocalDate.now().plusDays(3), new OptionChainData(112, CHAIN));
        assertEquals(110, view.atmStrike());
        assertEquals(1300, view.totalCallOi());
        assertEquals(1300, view.totalPutOi());
        assertEquals(1.0, view.pcr());
        assertEquals(100, view.support());
        assertEquals(120, view.resistance());
        assertEquals(110, view.maxPain());
        assertTrue(view.reading().contains("balanced"));
        assertEquals(75, view.lotSize());
    }

    @Test
    void maxPainIsWhereOptionBuyersGetPaidLeast() {
        // Payout if expiry = 100: calls 0; puts 300*10 + 100*20 = 5000
        // expiry = 110: calls 100*10 = 1000; puts 100*10 = 1000 -> 2000 (lowest)
        // expiry = 120: calls 100*20 + 300*10 = 5000; puts 0
        assertEquals(110, FnoService.maxPain(CHAIN));
    }

    @Test
    void pcrReadings() {
        assertTrue(FnoService.reading(1.5).contains("bullish"));
        assertTrue(FnoService.reading(0.5).contains("bearish"));
        assertTrue(FnoService.reading(1.0).contains("balanced"));
        assertTrue(FnoService.reading(null).contains("Not enough"));
    }
}
