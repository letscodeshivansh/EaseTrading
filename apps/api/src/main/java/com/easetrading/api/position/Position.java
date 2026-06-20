package com.easetrading.api.position;

import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

/**
 * A holding: how many shares of one instrument a user owns and at what average
 * price. Updated whenever an order fills. P&L is computed on the fly against the
 * latest market price (we don't store a stale P&L).
 */
@Entity
@Table(name = "position", uniqueConstraints = @UniqueConstraint(columnNames = {"userId", "token"}))
public class Position {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private UUID userId;

    @Column(nullable = false)
    private String token;

    private int qty;
    private double avgPrice;
    private Instant updatedAt = Instant.now();

    protected Position() { }

    public Position(UUID userId, String token) {
        this.userId = userId;
        this.token = token;
    }

    /**
     * Apply a BUY fill: blend the new shares into the weighted-average price.
     */
    public void applyBuy(int buyQty, double price) {
        double newCost = avgPrice * qty + price * buyQty;
        qty += buyQty;
        avgPrice = qty == 0 ? 0 : newCost / qty;
        updatedAt = Instant.now();
    }

    /**
     * Apply a SELL fill and return the realized profit/loss on the shares sold.
     */
    public double applySell(int sellQty, double price) {
        int sold = Math.min(sellQty, qty);
        double realized = (price - avgPrice) * sold;
        qty -= sold;
        if (qty == 0) avgPrice = 0;
        updatedAt = Instant.now();
        return realized;
    }

    public UUID getUserId() { return userId; }
    public String getToken() { return token; }
    public int getQty() { return qty; }
    public double getAvgPrice() { return avgPrice; }
}
