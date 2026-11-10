package io.github.regalpine.agentforge.security.audit;

import io.github.regalpine.agentforge.security.AuditEvent;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * AuditStore — AF-SEC-01 §57-59.
 *
 * SPI for persisting and querying audit events.
 * Audit events must NOT contain secrets (§59).
 *
 * Implementations:
 * - InMemoryAuditStore (testing)
 * - JdbcAuditStore (production)
 * - FileAuditStore (production)
 */
public interface AuditStore {

    /**
     * Store an audit event.
     */
    void store(AuditEvent event);

    /**
     * Find an audit event by ID.
     */
    Optional<AuditEvent> findById(String eventId);

    /**
     * Query events by tenant.
     */
    List<AuditEvent> findByTenant(
        String tenantId,
        int limit
    );

    /**
     * Query events by principal.
     */
    List<AuditEvent> findByPrincipal(
        String principalId,
        int limit
    );

    /**
     * Query events by action type.
     */
    List<AuditEvent> findByAction(
        AuditEvent.AuditAction action,
        int limit
    );

    /**
     * Query events in a time range.
     */
    List<AuditEvent> findByTimeRange(
        Instant from,
        Instant to,
        int limit
    );

    /**
     * Query events by execution ID.
     */
    List<AuditEvent> findByExecution(
        String executionId,
        int limit
    );

    /**
     * Delete events older than the retention period.
     *
     * @return number of events deleted
     */
    int purgeOlderThan(Instant cutoff);

    /**
     * Get the total count of stored events.
     */
    long count();
}
