package com.easetrading.api.order;

import com.easetrading.api.order.OrderEnums.Status;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Real-money execution. Active only when easetrading.trading.mode = live.
 *
 * DELIBERATELY BLOCKED until phase G5 of docs/GROWW_INTEGRATION_PLAN.md. Before real
 * orders are allowed we must have: login on order endpoints (B6), risk re-check and
 * draft expiry at confirm (B13), no ₹0 market orders (B14), a SELL holdings check
 * (B15), and order-status polling (B12). Until then every live order is rejected
 * with a clear message, so flipping TRADING_MODE=live by mistake can't move money.
 */
@Component
@ConditionalOnProperty(name = "easetrading.trading.mode", havingValue = "live")
public class LiveTradeExecutor implements TradeExecutor {

    public LiveTradeExecutor() {
        System.out.println("[Trading] LIVE mode requested — live orders are BLOCKED until phase G5.");
    }

    @Override
    public ExecutionResult execute(Order order, double ltp) {
        return new ExecutionResult(null, Status.REJECTED, 0,
                "Live trading is not enabled yet (phase G5). Use TRADING_MODE=paper.");
    }
}
