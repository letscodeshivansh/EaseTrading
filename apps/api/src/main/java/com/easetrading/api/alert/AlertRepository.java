package com.easetrading.api.alert;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface AlertRepository extends JpaRepository<Alert, UUID> {
    List<Alert> findByUserIdOrderByCreatedAtDesc(UUID userId);
    List<Alert> findByStatus(Alert.Status status);
}
