package com.easetrading.api.broker;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface BrokerCredentialRepository extends JpaRepository<BrokerCredential, UUID> {
    Optional<BrokerCredential> findByUserId(UUID userId);
}
