package com.easetrading.api.marketdata;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;

public interface CandleRepository extends JpaRepository<Candle, CandleId> {

    /** Bars for one instrument at one resolution, in time order. */
    List<Candle> findByTokenAndIntervalAndTsBetweenOrderByTsAsc(
            String token, String interval, Instant from, Instant to);

    boolean existsByTokenAndInterval(String token, String interval);
}
