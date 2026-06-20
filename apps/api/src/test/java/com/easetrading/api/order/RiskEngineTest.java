package com.easetrading.api.order;

import com.easetrading.api.config.AppProperties;
import com.easetrading.api.order.OrderEnums.Side;
import com.easetrading.api.order.OrderEnums.Status;
import com.easetrading.api.order.OrderEnums.Type;
import com.easetrading.api.position.PositionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

/**
 * Tests the pre-trade risk checks with mocked repositories, so we can assert exactly
 * how each rule behaves without a database.
 */
class RiskEngineTest {

    private RiskEngine engine;
    private final UUID user = UUID.randomUUID();

    @BeforeEach
    void setup() {
        AppProperties props = new AppProperties();
        // Defaults: maxOrderValue 100k, maxPositionPct 25, priceBand 10%, dailyLoss 25k.

        PositionRepository positions = Mockito.mock(PositionRepository.class);
        when(positions.findByUserId(any())).thenReturn(List.of());
        when(positions.findByUserIdAndToken(any(), any())).thenReturn(Optional.empty());

        OrderRepository orders = Mockito.mock(OrderRepository.class);
        when(orders.findByUserIdAndStatusAndCreatedAtAfter(any(), eq(Status.FILLED), any()))
                .thenReturn(List.of());

        engine = new RiskEngine(props, positions, orders);
    }

    @Test
    void smallOrderPassesAllChecks() {
        // 10 shares @ 1000 = 10,000 (well under all limits).
        Order order = new Order(user, "2885", "RELIANCE", Side.BUY, Type.MARKET, 10, 1000);
        RiskReport report = engine.check(user, order, 1000);
        assertTrue(report.passed());
    }

    @Test
    void oversizedOrderFailsMaxValue() {
        // 1000 shares @ 1000 = 1,000,000 (over the 100k limit).
        Order order = new Order(user, "2885", "RELIANCE", Side.BUY, Type.MARKET, 1000, 1000);
        RiskReport report = engine.check(user, order, 1000);
        assertFalse(report.passed());
        assertTrue(report.checks().stream()
                .anyMatch(c -> c.name().equals("Max order value") && !c.passed()));
    }

    @Test
    void limitPriceFarFromLtpFailsPriceBand() {
        // Limit 1500 vs LTP 1000 = 50% away (max 10%).
        Order order = new Order(user, "2885", "RELIANCE", Side.BUY, Type.LIMIT, 1, 1500);
        RiskReport report = engine.check(user, order, 1000);
        assertFalse(report.passed());
        assertTrue(report.checks().stream()
                .anyMatch(c -> c.name().equals("Price band") && !c.passed()));
    }
}
