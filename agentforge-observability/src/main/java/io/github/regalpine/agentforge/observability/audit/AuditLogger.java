package io.github.regalpine.agentforge.observability.audit;

import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Audit logger.
 * AF-ENHANCE-06 §8: records all audit
 * events for compliance and security.
 */
public final class AuditLogger {

    private final List<AuditEvent> events =
        new CopyOnWriteArrayList<>();

    private final List<AuditSink> sinks =
        new CopyOnWriteArrayList<>();

    /**
     * Add an audit sink.
     */
    public AuditLogger addSink(AuditSink sink) {
        sinks.add(
            Objects.requireNonNull(sink)
        );
        return this;
    }

    /**
     * Log an audit event.
     */
    public void log(AuditEvent event) {
        events.add(event);
        for (AuditSink sink : sinks) {
            try {
                sink.accept(event);
            } catch (Exception ignored) {}
        }
    }

    /**
     * Get all audit events.
     */
    public List<AuditEvent> events() {
        return List.copyOf(events);
    }

    /**
     * Get events for a specific tenant.
     */
    public List<AuditEvent> eventsForTenant(
        String tenantId
    ) {
        return events.stream()
            .filter(e -> tenantId.equals(
                e.tenantId()
            ))
            .toList();
    }

    /**
     * Get events for a specific action.
     */
    public List<AuditEvent> eventsForAction(
        AuditAction action
    ) {
        return events.stream()
            .filter(e -> e.action() == action)
            .toList();
    }

    /**
     * Get denied events.
     */
    public List<AuditEvent> deniedEvents() {
        return events.stream()
            .filter(e -> e.outcome()
                == AuditOutcome.DENIED)
            .toList();
    }

    /**
     * Get event count.
     */
    public int count() {
        return events.size();
    }

    /**
     * Clear all events.
     */
    public void clear() {
        events.clear();
    }

    /**
     * Audit event sink SPI.
     */
    public interface AuditSink {
        void accept(AuditEvent event);
    }
}
