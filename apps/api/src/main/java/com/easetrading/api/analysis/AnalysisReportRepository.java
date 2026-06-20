package com.easetrading.api.analysis;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface AnalysisReportRepository extends JpaRepository<AnalysisReport, UUID> {
    /** Recent reports for a stock, newest first. */
    List<AnalysisReport> findTop10ByTokenOrderByCreatedAtDesc(String token);
}
