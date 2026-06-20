package com.easetrading.api.fundamental;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

public interface FundamentalRepository extends JpaRepository<Fundamental, UUID> {
    Optional<Fundamental> findByTokenAndAsOf(String token, LocalDate asOf);
}
