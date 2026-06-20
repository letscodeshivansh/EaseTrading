package com.easetrading.api.analysis;

import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

/**
 * A stored analysis verdict. Persisting these gives an audit trail of every
 * recommendation the platform showed — important before any trade is placed
 * (Prompt 4) and useful for reviewing how calls played out over time.
 *
 * The full signal set (all the numbers) is kept as JSON text so we can show exactly
 * what the verdict was based on.
 */
@Entity
@Table(name = "analysis_report", indexes = {
        @Index(name = "idx_report_token", columnList = "token,createdAt")
})
public class AnalysisReport {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    // Nullable for now (single-user dev). Becomes required in the multi-user phase.
    private UUID userId;

    @Column(nullable = false)
    private String token;

    @Column(nullable = false)
    private String strategy;

    @Column(nullable = false)
    private String rating;

    private Double entry;
    private Double stopLoss;
    private Double target;
    private Double rrRatio;
    private Double confidence;

    /** "claude" or "grounded" — how the memo was produced. */
    private String source;

    @Column(columnDefinition = "text")
    private String memo;

    /** All computed signals as JSON (auditable). */
    @Column(columnDefinition = "text")
    private String signalsJson;

    @Column(nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    protected AnalysisReport() { }

    public AnalysisReport(String token, String strategy) {
        this.token = token;
        this.strategy = strategy;
    }

    public void setUserId(UUID v) { this.userId = v; }
    public void setRating(String v) { this.rating = v; }
    public void setEntry(Double v) { this.entry = v; }
    public void setStopLoss(Double v) { this.stopLoss = v; }
    public void setTarget(Double v) { this.target = v; }
    public void setRrRatio(Double v) { this.rrRatio = v; }
    public void setConfidence(Double v) { this.confidence = v; }
    public void setSource(String v) { this.source = v; }
    public void setMemo(String v) { this.memo = v; }
    public void setSignalsJson(String v) { this.signalsJson = v; }

    public UUID getId() { return id; }
    public String getToken() { return token; }
    public String getStrategy() { return strategy; }
    public String getRating() { return rating; }
    public Instant getCreatedAt() { return createdAt; }
}
