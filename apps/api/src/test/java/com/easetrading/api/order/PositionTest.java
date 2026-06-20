package com.easetrading.api.order;

import com.easetrading.api.position.Position;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests the holding math: weighted-average buys and realized P&L on sells.
 * Pure logic, no Spring context needed — fast and deterministic.
 */
class PositionTest {

    @Test
    void weightedAveragePriceOnMultipleBuys() {
        Position p = new Position(UUID.randomUUID(), "2885");
        p.applyBuy(10, 100);   // 10 @ 100
        p.applyBuy(10, 120);   // 10 @ 120
        // Average of 20 shares = (1000 + 1200) / 20 = 110.
        assertEquals(20, p.getQty());
        assertEquals(110.0, p.getAvgPrice(), 0.0001);
    }

    @Test
    void sellRealizesProfitAndReducesQty() {
        Position p = new Position(UUID.randomUUID(), "2885");
        p.applyBuy(10, 100);
        double realized = p.applySell(4, 130); // sell 4 @ 130, bought @ 100
        assertEquals(120.0, realized, 0.0001);  // (130-100) * 4
        assertEquals(6, p.getQty());
    }

    @Test
    void sellingAllClearsAveragePrice() {
        Position p = new Position(UUID.randomUUID(), "2885");
        p.applyBuy(5, 200);
        p.applySell(5, 180);
        assertEquals(0, p.getQty());
        assertEquals(0.0, p.getAvgPrice(), 0.0001);
    }
}
