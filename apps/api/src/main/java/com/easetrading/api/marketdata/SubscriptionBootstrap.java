package com.easetrading.api.marketdata;

import com.easetrading.api.market.MarketService;
import com.easetrading.api.watchlist.WatchlistItemRepository;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Decides which stocks stream live prices after startup:
 *   default stocks + every stock on anyone's watchlist + the indices and Nifty 50
 *   stocks shown on the dashboard.
 * Each user's holdings start streaming when they open their portfolio.
 *
 * Runs AFTER InstrumentSync (@Order(2) vs @Order(1)), so the stock list exists
 * before we look tokens up.
 */
@Component
@Order(2)
public class SubscriptionBootstrap implements ApplicationRunner {

    private static final List<String> DEFAULTS = List.of("2885", "11536", "1333");

    private final MarketSubscription subscription;
    private final WatchlistItemRepository watchlists;
    private final MarketService market;

    public SubscriptionBootstrap(MarketSubscription subscription, WatchlistItemRepository watchlists,
                                 MarketService market) {
        this.subscription = subscription;
        this.watchlists = watchlists;
        this.market = market;
    }

    @Override
    public void run(ApplicationArguments args) {
        DEFAULTS.forEach(subscription::subscribeToken);
        watchlists.findAllTokens().forEach(subscription::subscribeToken);
        market.trackedInstruments().forEach(subscription::add); // indices + Nifty 50 for the dashboard
        System.out.printf("[Startup] Streaming %d stocks.%n", subscription.all().size());
    }
}
