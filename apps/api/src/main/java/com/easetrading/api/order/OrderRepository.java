package com.easetrading.api.order;

import com.easetrading.api.order.OrderEnums.Status;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface OrderRepository extends JpaRepository<Order, UUID> {
    List<Order> findTop50ByUserIdOrderByCreatedAtDesc(UUID userId);
    Optional<Order> findByIdAndUserId(UUID id, UUID userId);

    /** Filled orders since a time — used to sum today's realized P&L for the loss limit. */
    List<Order> findByUserIdAndStatusAndCreatedAtAfter(UUID userId, Status status, Instant since);
}
