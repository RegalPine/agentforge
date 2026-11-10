package io.github.regalpine.agentforge.observability.audit;

import java.time.Instant;
import java.util.Map;
import java.util.Objects;

/**
 * Audit event.
 * AF-ENHANCE-06 §8: unified audit
 * event for operations, policy decisions,
 * and security events.
 */
public record AuditEvent(
    String eventId,
    AuditAction action,
    String actor,
    String tenantId,
    String resourceId,
    Instant timestamp,
    AuditOutcome outcome,
    Map<String, String> details
) {

    public AuditEvent {
        Objects.requireNonNull(eventId);
        Objects.requireNonNull(action);
        Objects.requireNonNull(actor);
        timestamp = timestamp == null
            ? Instant.now() : timestamp;
        outcome = outcome == null
            ? AuditOutcome.SUCCESS : outcome;
        details = details == null
            ? Map.of() : Map.copyOf(details);
    }

    /**
     * Create a success audit event.
     */
    public static AuditEvent success(
        String eventId,
        AuditAction action,
        String actor,
        String tenantId
    ) {
        return new AuditEvent(
            eventId, action, actor,
            tenantId, null,
            Instant.now(),
            AuditOutcome.SUCCESS,
            Map.of()
        );
    }

    /**
     * Create a denied audit event.
     */
    public static AuditEvent denied(
        String eventId,
        AuditAction action,
        String actor,
        String tenantId,
        String reason
    ) {
        return new AuditEvent(
            eventId, action, actor,
            tenantId, null,
            Instant.now(),
            AuditOutcome.DENIED,
            Map.of("reason", reason)
        );
    }
}
