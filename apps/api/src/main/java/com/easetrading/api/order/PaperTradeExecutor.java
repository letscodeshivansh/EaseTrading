package com.easetrading.api.order;

import com.easetrading.api.order.OrderEnums.Status;
import com.easetrading.api.order.OrderEnums.Type;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Simulated execution — the SAFE default. No real money moves. A market order fills
 * at the current price; a limit order fills at its limit price. This lets you test
 * the entire order workflow end-to-end before ever going live.
 *
 * Active when easetrading.trading.mode = paper (the default).
 */
@Component
@ConditionalOnProperty(name = "easetrading.trading.mode", havingValue = "paper", matchIfMissing = true)
public class PaperTradeExecutor implements TradeExecutor {

    public PaperTradeExecutor() {
        System.out.println("[Trading] PAPER mode — orders are simulated, no real money.");
    }

    @Override
    public ExecutionResult execute(Order order, double ltp) {
        double fill = order.getType() == Type.MARKET && ltp > 0 ? ltp : order.getPrice();
        return new ExecutionResult("PAPER-" + UUID.randomUUID(), Status.FILLED, fill, "Simulated fill");
    }
}
