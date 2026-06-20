package com.easetrading.api.audit;

import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

/**
 * An immutable record of a significant action — every order step and every verdict
 * shown before a trade. This is the trail you can review later to answer "what did
 * the system do, and why?". Critical for anything touching real money.
 */
@Entity
@Table(name = "audit_log", indexes = @Index(name = "idx_audit_user", columnList = "userId,createdAt"))
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    private UUID userId;
    private String action;     // e.g. ORDER_DRAFTED, ORDER_CONFIRMED, ALERT_TRIGGERED
    private String entity;     // e.g. ORDER, ALERT
    private String entityId;

    @Column(columnDefinition = "text")
    private String detail;     // free-text / JSON detail

    @Column(nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    protected AuditLog() { }

    public AuditLog(UUID userId, String action, String entity, String entityId, String detail) {
        this.userId = userId;
        this.action = action;
        this.entity = entity;
        this.entityId = entityId;
        this.detail = detail;
    }

    public Instant getCreatedAt() { return createdAt; }
    public String getAction() { return action; }
}
