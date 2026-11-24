package com.easetrading.api.watchlist;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface WatchlistItemRepository extends JpaRepository<WatchlistItem, UUID> {

    List<WatchlistItem> findByUserIdOrderBySortOrderAscCreatedAtAsc(UUID userId);

    Optional<WatchlistItem> findByUserIdAndToken(UUID userId, String token);

    long countByUserId(UUID userId);

    /** Every token on any watchlist — streamed at startup. */
    @Query("select distinct w.token from WatchlistItem w")
    List<String> findAllTokens();
}
