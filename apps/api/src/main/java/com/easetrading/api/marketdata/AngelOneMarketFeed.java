package com.easetrading.api.marketdata;

import com.easetrading.api.broker.AngelOneAdapter;
import com.easetrading.api.broker.AngelOneSession;
import com.easetrading.api.broker.BrokerSessionProvider;
import com.easetrading.api.instrument.Instrument;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;

/**
 * Live market feed. Active only when easetrading.market.feed-mode = live.
 *
 * For Prompt 1 this polls Angel One's last-traded-price REST endpoint once per
 * second for each subscribed instrument. It is deliberately simple and robust.
 *
 * UPGRADE PATH: replace the polling loop with Angel One's SmartWebSocket v2 for true
 * tick-by-tick data. Because everything downstream only depends on QuoteCache.publish(),
 * that swap touches this class only.
 */
@Component
@ConditionalOnProperty(name = "easetrading.market.feed-mode", havingValue = "live")
public class AngelOneMarketFeed {

    private final MarketSubscription subscription;
    private final QuoteCache quoteCache;
    private final AngelOneAdapter angelOne;
    private final BrokerSessionProvider sessionProvider;

    public AngelOneMarketFeed(MarketSubscription subscription, QuoteCache quoteCache,
                              AngelOneAdapter angelOne, BrokerSessionProvider sessionProvider) {
        this.subscription = subscription;
        this.quoteCache = quoteCache;
        this.angelOne = angelOne;
        this.sessionProvider = sessionProvider;
        System.out.println("[MarketFeed] LIVE feed active — polling Angel One SmartAPI.");
    }

    @Scheduled(fixedRate = 1000)
    public void pollPrices() {
        AngelOneSession session;
        try {
            session = sessionProvider.get();
        } catch (Exception e) {
            System.err.println("[MarketFeed] Login failed, will retry: " + e.getMessage());
            return;
        }

        for (Instrument instrument : subscription.all().values()) {
            try {
                double ltp = angelOne.getLastTradedPrice(
                        session, instrument.getExchange(), instrument.getSymbol(), instrument.getToken());
                quoteCache.publish(new Tick(instrument.getToken(), ltp, Instant.now()));
            } catch (Exception e) {
                // If the token expired, force a fresh login next cycle.
                sessionProvider.invalidate();
                System.err.println("[MarketFeed] LTP poll failed for "
                        + instrument.getSymbol() + ": " + e.getMessage());
            }
        }
    }
}
