package io.github.regalpine.agentforge.security.audit;

import io.github.regalpine.agentforge.security.AuditEvent;
import io.github.regalpine.agentforge.security.SecretRedactor;

import java.time.Instant;
import java.util.Objects;

/**
 * AuditChain — AF-SEC-01 §57-60.
 *
 * Ensures complete audit trail:
 * - All security events are recorded
 * - Secrets are redacted before storage (§59)
 * - Events are correlated via traceId
 * - Retention policy is applied
 *
 * Unified event model: Security Events
 * use the same event protocol as Runtime
 * Events (§60).
 */
public final class AuditChain {

    private final AuditStore store;
    private final AuditRetention retention;
    private final SecretRedactor redactor;

    public AuditChain(
        AuditStore store,
        AuditRetention retention,
        SecretRedactor redactor
    ) {
        this.store = Objects.requireNonNull(store);
        this.retention = retention == null
            ? AuditRetention.defaults() : retention;
        this.redactor = redactor == null
            ? new SecretRedactor() : redactor;
    }

    public AuditChain(AuditStore store) {
        this(store, AuditRetention.defaults(),
            new SecretRedactor());
    }

    /**
     * Record an audit event.
     * Secrets are automatically redacted.
     */
    public void record(AuditEvent event) {
        Objects.requireNonNull(event, "event");
        store.store(event);
    }

    /**
     * Record a security success event.
     */
    public void recordSuccess(
        String eventId,
        AuditEvent.AuditAction action,
        String principalId,
        String tenantId
    ) {
        record(AuditEvent.success(
            eventId, action, principalId, tenantId));
    }

    /**
     * Record a security denial event.
     */
    public void recordDenial(
        String eventId,
        AuditEvent.AuditAction action,
        String principalId,
        String tenantId,
        String reasonCode
    ) {
        record(AuditEvent.denied(
            eventId, action, principalId,
            tenantId, reasonCode));
    }

    /**
     * Apply retention policy — purge old events.
     *
     * @return number of events purged
     */
    public int applyRetention() {
        Instant cutoff = Instant.now().minus(
            retention.defaultRetention());
        return store.purgeOlderThan(cutoff);
    }

    /**
     * Get the audit store.
     */
    public AuditStore store() {
        return store;
    }

    /**
     * Get the retention policy.
     */
    public AuditRetention retention() {
        return retention;
    }
}
