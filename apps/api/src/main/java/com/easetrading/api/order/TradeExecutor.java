package com.easetrading.api.order;

import com.easetrading.api.order.OrderEnums.Status;

/**
 * Abstraction over "how an order actually gets executed". Two implementations:
 *   - PaperTradeExecutor : simulates fills (safe default, for testing)
 *   - LiveTradeExecutor  : sends real orders to Angel One
 * The active one is chosen by easetrading.trading.mode.
 */
public interface TradeExecutor {

    /** The outcome of trying to execute an order. */
    record ExecutionResult(String brokerOrderId, Status status, double fillPrice, String message) {}

    ExecutionResult execute(Order order, double ltp);
}
