package com.easetrading.api.alert;

import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

/**
 * A user-defined alert, e.g. "tell me when RELIANCE goes above 1500" or
 * "when RSI drops below 30". Evaluated continuously by AlertService.
 */
@Entity
@Table(name = "alert", indexes = @Index(name = "idx_alert_status", columnList = "status"))
public class Alert {

    public enum Type { PRICE, RSI }
    public enum Operator { GT, LT }       // greater-than / less-than
    public enum Status { ACTIVE, TRIGGERED, DISABLED }

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private UUID userId;

    @Column(nullable = false)
    private String token;

    private String symbol;

    @Enumerated(EnumType.STRING) @Column(nullable = false)
    private Type type;

    @Enumerated(EnumType.STRING) @Column(nullable = false)
    private Operator operator;

    private double threshold;

    @Enumerated(EnumType.STRING) @Column(nullable = false)
    private Status status = Status.ACTIVE;

    private Double lastValue;        // most recent observed value
    private Instant triggeredAt;

    @Column(nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    protected Alert() { }

    public Alert(UUID userId, String token, String symbol, Type type, Operator operator, double threshold) {
        this.userId = userId;
        this.token = token;
        this.symbol = symbol;
        this.type = type;
        this.operator = operator;
        this.threshold = threshold;
    }

    /** Does the observed value satisfy this alert's condition? */
    public boolean isTriggeredBy(double value) {
        return operator == Operator.GT ? value > threshold : value < threshold;
    }

    public void trigger(double value) {
        this.status = Status.TRIGGERED;
        this.lastValue = value;
        this.triggeredAt = Instant.now();
    }

    public UUID getId() { return id; }
    public UUID getUserId() { return userId; }
    public String getToken() { return token; }
    public String getSymbol() { return symbol; }
    public Type getType() { return type; }
    public Operator getOperator() { return operator; }
    public double getThreshold() { return threshold; }
    public Status getStatus() { return status; }
    public void setLastValue(Double v) { this.lastValue = v; }
}
