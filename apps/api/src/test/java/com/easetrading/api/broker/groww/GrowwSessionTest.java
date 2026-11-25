package com.easetrading.api.broker.groww;

import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

class GrowwSessionTest {

    @Test
    void checksumIsSha256OfSecretPlusTimestamp() {
        // Known vector: SHA-256("abc").
        assertEquals("ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad",
                GrowwSession.checksum("ab", "c"));
    }

    @Test
    void tokenExpiresAtNextSixAmIst() {
        // 2026-01-10 05:00 IST -> expires same day 06:00 IST (00:30 UTC).
        assertEquals(Instant.parse("2026-01-10T00:30:00Z"),
                GrowwSession.nextExpiry(Instant.parse("2026-01-09T23:30:00Z")));
        // 2026-01-10 10:00 IST -> expires next day 06:00 IST.
        assertEquals(Instant.parse("2026-01-11T00:30:00Z"),
                GrowwSession.nextExpiry(Instant.parse("2026-01-10T04:30:00Z")));
    }

    @Test
    void parsedExpiryNeverExceedsSixAmCutoff() {
        Instant cutoff = GrowwSession.nextExpiry(Instant.now());
        assertEquals(cutoff, GrowwSession.parseExpiry("2099-01-01T00:00:00"));
        assertEquals(cutoff, GrowwSession.parseExpiry(null));
        assertEquals(cutoff, GrowwSession.parseExpiry("garbage"));
    }

}
