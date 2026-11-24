package com.easetrading.api.marketdata;

import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

class MarketHoursTest {

    @Test
    void openOnWeekdayDuringSession() {
        // Wed 2026-01-07 10:00 IST = 04:30 UTC
        assertTrue(MarketHours.isOpen(Instant.parse("2026-01-07T04:30:00Z")));
    }

    @Test
    void closedBeforeOpenAfterCloseAndOnWeekends() {
        assertFalse(MarketHours.isOpen(Instant.parse("2026-01-07T03:40:00Z"))); // 09:10 IST
        assertFalse(MarketHours.isOpen(Instant.parse("2026-01-07T10:00:00Z"))); // 15:30 IST
        assertFalse(MarketHours.isOpen(Instant.parse("2026-01-10T04:30:00Z"))); // Saturday
    }
}
