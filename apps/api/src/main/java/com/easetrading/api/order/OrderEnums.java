package com.easetrading.api.order;

/** Small enums describing an order. Kept together for convenience. */
public class OrderEnums {

    public enum Side { BUY, SELL }

    public enum Type { MARKET, LIMIT }

    /**
     * The order lifecycle (state machine):
     *
     *   DRAFT --(risk passes + user confirms)--> PENDING_CONFIRM
     *   PENDING_CONFIRM --(sent to broker)-----> SUBMITTED
     *   SUBMITTED --(broker accepts)-----------> OPEN
     *   OPEN --(executes)----------------------> FILLED
     *   SUBMITTED/OPEN ------------------------> REJECTED | CANCELLED
     */
    public enum Status { DRAFT, PENDING_CONFIRM, SUBMITTED, OPEN, FILLED, REJECTED, CANCELLED }
}
