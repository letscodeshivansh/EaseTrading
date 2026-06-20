package com.easetrading.api.order;

import com.easetrading.api.config.AppProperties;
import com.easetrading.api.order.OrderEnums.Side;
import com.easetrading.api.order.OrderEnums.Status;
import com.easetrading.api.order.OrderEnums.Type;
import com.easetrading.api.order.RiskReport.RiskCheck;
import com.easetrading.api.position.Position;
import com.easetrading.api.position.PositionRepository;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Pre-trade risk checks. Every order must pass these before it can be confirmed.
 * The checks are deliberately conservative — protecting real money is the priority.
 */
@Component
public class RiskEngine {

    private static final ZoneId IST = ZoneId.of("Asia/Kolkata");

    private final AppProperties props;
    private final PositionRepository positions;
    private final OrderRepository orders;

    public RiskEngine(AppProperties props, PositionRepository positions, OrderRepository orders) {
        this.props = props;
        this.positions = positions;
        this.orders = orders;
    }

    public RiskReport check(UUID userId, Order order, double ltp) {
        var r = props.getRisk();
        List<RiskCheck> checks = new ArrayList<>();

        // 1. Quantity sanity.
        checks.add(new RiskCheck("Quantity",
                order.getQty() > 0,
                "Quantity = " + order.getQty()));

        // 2. Max order value.
        double value = order.notionalValue();
        checks.add(new RiskCheck("Max order value",
                value <= r.getMaxOrderValue(),
                String.format("Order value ₹%.0f (limit ₹%.0f)", value, r.getMaxOrderValue())));

        // 3. Price band (limit orders only): price must be within +/-X% of the LTP.
        if (order.getType() == Type.LIMIT && ltp > 0) {
            double deviation = Math.abs(order.getPrice() - ltp) / ltp * 100;
            checks.add(new RiskCheck("Price band",
                    deviation <= r.getPriceBandPct(),
                    String.format("Limit %.2f vs LTP %.2f = %.1f%% away (max %.0f%%)",
                            order.getPrice(), ltp, deviation, r.getPriceBandPct())));
        }

        // 4. Position concentration (buys only).
        if (order.getSide() == Side.BUY) {
            double existing = positions.findByUserIdAndToken(userId, order.getToken())
                    .map(p -> p.getQty() * p.getAvgPrice()).orElse(0.0);
            double newPosValue = existing + value;
            double base = Math.max(portfolioValue(userId), r.getMaxOrderValue() * 5); // notional floor
            double pct = newPosValue / base * 100;
            checks.add(new RiskCheck("Position concentration",
                    pct <= r.getMaxPositionPct(),
                    String.format("This stock would be %.1f%% of book (max %.0f%%)", pct, r.getMaxPositionPct())));

            // 5. Daily loss limit — block new buys after a bad day.
            double todayLoss = todayRealizedPnl(userId);
            checks.add(new RiskCheck("Daily loss limit",
                    todayLoss > -r.getDailyLossLimit(),
                    String.format("Today's realized P&L ₹%.0f (stop at -₹%.0f)", todayLoss, r.getDailyLossLimit())));
        }

        boolean passed = checks.stream().allMatch(RiskCheck::passed);
        return new RiskReport(passed, checks);
    }

    /** Rough portfolio value from holdings at their average cost. */
    private double portfolioValue(UUID userId) {
        return positions.findByUserId(userId).stream()
                .mapToDouble(p -> p.getQty() * p.getAvgPrice())
                .sum();
    }

    /** Sum of realized P&L from today's filled orders (negative = loss). */
    private double todayRealizedPnl(UUID userId) {
        var startOfDay = LocalDate.now(IST).atStartOfDay(IST).toInstant();
        return orders.findByUserIdAndStatusAndCreatedAtAfter(userId, Status.FILLED, startOfDay).stream()
                .mapToDouble(Order::getRealizedPnl)
                .sum();
    }
}
