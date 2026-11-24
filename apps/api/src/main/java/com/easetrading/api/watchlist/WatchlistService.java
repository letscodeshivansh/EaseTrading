package com.easetrading.api.watchlist;

import com.easetrading.api.common.ApiException;
import com.easetrading.api.common.CurrentUserService;
import com.easetrading.api.instrument.Instrument;
import com.easetrading.api.instrument.InstrumentStore;
import com.easetrading.api.marketdata.MarketSubscription;
import com.easetrading.api.marketdata.QuoteCache;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * The current user's watchlist. Adding a stock also starts streaming its price.
 * A brand-new user starts with RELIANCE, TCS and HDFCBANK so the dashboard isn't empty.
 */
@Service
public class WatchlistService {

    static final List<String> DEFAULT_TOKENS = List.of("2885", "11536", "1333");
    private static final int MAX_ITEMS = 100;

    private final WatchlistItemRepository repo;
    private final InstrumentStore instruments;
    private final MarketSubscription subscription;
    private final QuoteCache quotes;
    private final CurrentUserService currentUser;

    public WatchlistService(WatchlistItemRepository repo, InstrumentStore instruments,
                            MarketSubscription subscription, QuoteCache quotes,
                            CurrentUserService currentUser) {
        this.repo = repo;
        this.instruments = instruments;
        this.subscription = subscription;
        this.quotes = quotes;
        this.currentUser = currentUser;
    }

    /** What the UI shows per row. ltp may be 0 until the first price arrives. */
    public record WatchRow(String token, String symbol, String name, String exchange, double ltp) {}

    @Transactional
    public List<WatchRow> list() {
        UUID userId = currentUser.currentUserId();
        if (repo.countByUserId(userId) == 0) seedDefaults(userId);

        return repo.findByUserIdOrderBySortOrderAscCreatedAtAsc(userId).stream()
                .map(w -> instruments.get(w.getToken()).orElse(null))
                .filter(i -> i != null)
                .map(i -> new WatchRow(i.getToken(), i.getSymbol(), i.getName(), i.getExchange(),
                        quotes.latestPrice(i.getToken())))
                .toList();
    }

    @Transactional
    public List<WatchRow> add(String token) {
        UUID userId = currentUser.currentUserId();
        Instrument inst = instruments.get(token)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Unknown instrument: " + token));
        if (repo.findByUserIdAndToken(userId, token).isEmpty()) {
            long count = repo.countByUserId(userId);
            if (count >= MAX_ITEMS) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "Watchlist is full (max " + MAX_ITEMS + ")");
            }
            repo.save(new WatchlistItem(userId, token, (int) count));
        }
        subscription.add(inst);
        return list();
    }

    @Transactional
    public List<WatchRow> remove(String token) {
        UUID userId = currentUser.currentUserId();
        repo.findByUserIdAndToken(userId, token).ifPresent(repo::delete);
        return list();
    }

    private void seedDefaults(UUID userId) {
        int order = 0;
        for (String token : DEFAULT_TOKENS) {
            if (instruments.exists(token)) repo.save(new WatchlistItem(userId, token, order++));
        }
    }
}
