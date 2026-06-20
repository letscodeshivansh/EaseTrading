package com.easetrading.api.order;

import com.easetrading.api.order.OrderEnums.Side;
import com.easetrading.api.order.OrderEnums.Status;
import com.easetrading.api.order.OrderEnums.Type;
import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

/**
 * An order — the system of record. Note the table name is quoted ("orders") because
 * "order" is a reserved SQL word.
 *
 * An order starts as a DRAFT with a risk report attached, and only moves toward the
 * broker after an explicit human confirmation. The AI can never create anything
 * beyond a DRAFT.
 */
@Entity
@Table(name = "orders", indexes = @Index(name = "idx_orders_user", columnList = "userId,createdAt"))
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private UUID userId;

    @Column(nullable = false)
    private String token;

    @Column(nullable = false)
    private String symbol;

    @Enumerated(EnumType.STRING) @Column(nullable = false)
    private Side side;

    @Enumerated(EnumType.STRING) @Column(nullable = false)
    private Type type;

    private int qty;
    private double price;        // limit price (or the LTP snapshot for market orders)
    private double filledPrice;  // actual execution price

    @Enumerated(EnumType.STRING) @Column(nullable = false)
    private Status status = Status.DRAFT;

    private String brokerOrderId;
    private boolean riskPassed;

    @Column(columnDefinition = "text")
    private String riskJson;     // the full risk report (auditable)

    private double realizedPnl;  // set when a SELL fills

    @Column(nullable = false, updatable = false)
    private Instant createdAt = Instant.now();
    private Instant updatedAt = Instant.now();

    protected Order() { }

    public Order(UUID userId, String token, String symbol, Side side, Type type, int qty, double price) {
        this.userId = userId;
        this.token = token;
        this.symbol = symbol;
        this.side = side;
        this.type = type;
        this.qty = qty;
        this.price = price;
    }

    /** The notional value of the order, used by risk checks. */
    public double notionalValue() { return qty * price; }

    public void markStatus(Status s) { this.status = s; this.updatedAt = Instant.now(); }

    public UUID getId() { return id; }
    public UUID getUserId() { return userId; }
    public String getToken() { return token; }
    public String getSymbol() { return symbol; }
    public Side getSide() { return side; }
    public Type getType() { return type; }
    public int getQty() { return qty; }
    public double getPrice() { return price; }
    public double getFilledPrice() { return filledPrice; }
    public void setFilledPrice(double v) { this.filledPrice = v; }
    public Status getStatus() { return status; }
    public String getBrokerOrderId() { return brokerOrderId; }
    public void setBrokerOrderId(String v) { this.brokerOrderId = v; }
    public boolean isRiskPassed() { return riskPassed; }
    public void setRiskPassed(boolean v) { this.riskPassed = v; }
    public String getRiskJson() { return riskJson; }
    public void setRiskJson(String v) { this.riskJson = v; }
    public double getRealizedPnl() { return realizedPnl; }
    public void setRealizedPnl(double v) { this.realizedPnl = v; }
    public Instant getCreatedAt() { return createdAt; }
}
