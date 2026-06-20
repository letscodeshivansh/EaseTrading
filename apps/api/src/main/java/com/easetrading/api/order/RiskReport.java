package com.easetrading.api.order;

import java.util.List;

/**
 * The result of the pre-trade risk checks. Returned to the user on the review screen
 * so they see exactly why an order is (or isn't) allowed before confirming.
 */
public record RiskReport(boolean passed, List<RiskCheck> checks) {

    /** One named check with its outcome and a human-readable explanation. */
    public record RiskCheck(String name, boolean passed, String detail) {}
}
