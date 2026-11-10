package io.github.regalpine.agentforge.security;

import java.util.List;

/**
 * Audit log interface for recording security events.
 * <p>
 * Implementations can write to file, database, or external audit system.
 */
public interface AuditLog {

    /**
     * Record an audit event.
     *
     * @param event the audit event
     */
    void record(AuditEvent event);

    /**
     * Get recent audit events (most recent first).
     *
     * @param limit maximum number of events to return
     * @return list of recent events
     */
    List<AuditEvent> recent(int limit);

    /**
     * An in-memory audit log for development and testing.
     */
    static AuditLog inMemory() {
        return new InMemoryAuditLog();
    }
}
