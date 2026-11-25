package com.easetrading.api.marketdata;

import com.easetrading.api.watchlist.WatchlistItemRepository;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Decides which stocks stream live prices after startup:
 *   default stocks + every stock on anyone's watchlist.
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

    public SubscriptionBootstrap(MarketSubscription subscription, WatchlistItemRepository watchlists) {
        this.subscription = subscription;
        this.watchlists = watchlists;
    }

    @Override
    public void run(ApplicationArguments args) {
        DEFAULTS.forEach(subscription::subscribeToken);
        watchlists.findAllTokens().forEach(subscription::subscribeToken);
        System.out.printf("[Startup] Streaming %d stocks.%n", subscription.all().size());
    }
}
