package com.easetrading.api.audit;

import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

/** Tiny helper so any service can record an audit entry in one line. */
@Service
public class AuditService {

    private final AuditLogRepository repo;

    public AuditService(AuditLogRepository repo) {
        this.repo = repo;
    }

    public void log(UUID userId, String action, String entity, String entityId, String detail) {
        repo.save(new AuditLog(userId, action, entity, entityId, detail));
    }

    public List<AuditLog> recent(UUID userId) {
        return repo.findTop50ByUserIdOrderByCreatedAtDesc(userId);
    }
}
