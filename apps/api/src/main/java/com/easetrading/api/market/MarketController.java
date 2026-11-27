package com.easetrading.api.market;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Market overview (same for every logged-in user).
 *   GET /api/market/overview -> indices, Nifty 50 gainers/losers, breadth, market open?
 */
@RestController
@RequestMapping("/api/market")
public class MarketController {

    private final MarketService market;

    public MarketController(MarketService market) {
        this.market = market;
    }

    @GetMapping("/overview")
    public MarketService.Overview overview() {
        return market.overview();
    }
}
