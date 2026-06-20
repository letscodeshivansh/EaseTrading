package com.easetrading.api.fundamental;

import jakarta.persistence.*;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Stored fundamentals snapshot for one instrument on one date. Because fundamentals
 * change slowly (quarterly filings, daily ratios), we cache them here and refresh on
 * a schedule instead of hitting Yahoo/NSE on every page view.
 */
@Entity
@Table(name = "fundamental", uniqueConstraints =
        @UniqueConstraint(columnNames = {"token", "asOf"}))
public class Fundamental {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private String token;

    @Column(nullable = false)
    private LocalDate asOf;

    private Double peRatio;
    private Double eps;
    private Double roePct;
    private Double profitGrowthPct;
    private Double fiiHoldingPct;
    private Double diiHoldingPct;
    private Double promoterPct;
    private Double debtToEquity;
    private String source;

    protected Fundamental() { }

    public Fundamental(String token, LocalDate asOf) {
        this.token = token;
        this.asOf = asOf;
    }

    // Setters used when mapping from the analysis-service response.
    public void setPeRatio(Double v) { this.peRatio = v; }
    public void setEps(Double v) { this.eps = v; }
    public void setRoePct(Double v) { this.roePct = v; }
    public void setProfitGrowthPct(Double v) { this.profitGrowthPct = v; }
    public void setFiiHoldingPct(Double v) { this.fiiHoldingPct = v; }
    public void setDiiHoldingPct(Double v) { this.diiHoldingPct = v; }
    public void setPromoterPct(Double v) { this.promoterPct = v; }
    public void setDebtToEquity(Double v) { this.debtToEquity = v; }
    public void setSource(String v) { this.source = v; }

    public String getToken() { return token; }
    public LocalDate getAsOf() { return asOf; }
    public Double getPeRatio() { return peRatio; }
    public Double getEps() { return eps; }
    public Double getRoePct() { return roePct; }
    public Double getProfitGrowthPct() { return profitGrowthPct; }
    public Double getFiiHoldingPct() { return fiiHoldingPct; }
    public Double getDiiHoldingPct() { return diiHoldingPct; }
    public Double getPromoterPct() { return promoterPct; }
    public Double getDebtToEquity() { return debtToEquity; }
    public String getSource() { return source; }
}
