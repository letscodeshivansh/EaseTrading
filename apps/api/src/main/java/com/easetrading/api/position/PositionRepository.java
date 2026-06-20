package com.easetrading.api.position;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PositionRepository extends JpaRepository<Position, UUID> {
    List<Position> findByUserId(UUID userId);
    Optional<Position> findByUserIdAndToken(UUID userId, String token);
}
