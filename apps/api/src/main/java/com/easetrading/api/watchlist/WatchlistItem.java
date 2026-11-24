package com.easetrading.api.watchlist;

import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

/**
 * One stock on a user's watchlist. One default watchlist per user for now;
 * a `listName` column can be added later for multiple named lists.
 */
@Entity
@Table(name = "watchlist_item", uniqueConstraints = @UniqueConstraint(columnNames = {"userId", "token"}))
public class WatchlistItem {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private UUID userId;

    @Column(nullable = false)
    private String token;

    private int sortOrder;

    @Column(nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    protected WatchlistItem() { }

    public WatchlistItem(UUID userId, String token, int sortOrder) {
        this.userId = userId;
        this.token = token;
        this.sortOrder = sortOrder;
    }

    public UUID getUserId() { return userId; }
    public String getToken() { return token; }
    public int getSortOrder() { return sortOrder; }
}
