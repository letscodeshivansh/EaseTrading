package com.easetrading.api.marketdata;

import com.easetrading.api.broker.BrokerAdapter;
import com.easetrading.api.instrument.InstrumentResolver;
import com.easetrading.api.watchlist.WatchlistItemRepository;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Decides which stocks stream live prices after startup:
 *   default stocks + every watchlist stock + every stock you hold.
 *
 * Runs AFTER InstrumentSync (@Order(2) vs @Order(1)), so the stock list exists
 * before we look tokens up — this is the fix for B7.
 */
@Component
@Order(2)
public class SubscriptionBootstrap implements ApplicationRunner {

    private static final List<String> DEFAULTS = List.of("2885", "11536", "1333");

    private final MarketSubscription subscription;
    private final WatchlistItemRepository watchlists;
    private final BrokerAdapter broker;
    private final InstrumentResolver resolver;

    public SubscriptionBootstrap(MarketSubscription subscription, WatchlistItemRepository watchlists,
                                 BrokerAdapter broker, InstrumentResolver resolver) {
        this.subscription = subscription;
        this.watchlists = watchlists;
        this.broker = broker;
        this.resolver = resolver;
    }

    @Override
    public void run(ApplicationArguments args) {
        DEFAULTS.forEach(subscription::subscribeToken);
        watchlists.findAllTokens().forEach(subscription::subscribeToken);
        try {
            broker.holdings().forEach(h ->
                    resolver.resolve(h.tradingSymbol(), h.isin()).ifPresent(subscription::add));
        } catch (Exception e) {
            System.err.println("[Startup] Could not load holdings for streaming: " + e.getMessage());
        }
        System.out.printf("[Startup] Streaming %d stocks.%n", subscription.all().size());
    }
}
