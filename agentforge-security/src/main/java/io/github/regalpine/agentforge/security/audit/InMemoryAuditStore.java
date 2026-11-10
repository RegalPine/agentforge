package io.github.regalpine.agentforge.security.audit;

import io.github.regalpine.agentforge.security.AuditEvent;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * InMemoryAuditStore — AF-SEC-01 §57-59.
 *
 * In-memory implementation for testing.
 * NOT suitable for production.
 */
public final class InMemoryAuditStore
    implements AuditStore {

    private final Map<String, AuditEvent> events
        = new ConcurrentHashMap<>();

    private final List<AuditEvent> ordered
        = new CopyOnWriteArrayList<>();

    @Override
    public void store(AuditEvent event) {
        Objects.requireNonNull(event, "event");
        events.put(event.eventId(), event);
        ordered.add(event);
    }

    @Override
    public Optional<AuditEvent> findById(String eventId) {
        return Optional.ofNullable(
            events.get(eventId));
    }

    @Override
    public List<AuditEvent> findByTenant(
        String tenantId,
        int limit
    ) {
        return ordered.stream()
            .filter(e -> tenantId.equals(
                e.tenantId()))
            .limit(limit)
            .toList();
    }

    @Override
    public List<AuditEvent> findByPrincipal(
        String principalId,
        int limit
    ) {
        return ordered.stream()
            .filter(e -> principalId.equals(
                e.principalId()))
            .limit(limit)
            .toList();
    }

    @Override
    public List<AuditEvent> findByAction(
        AuditEvent.AuditAction action,
        int limit
    ) {
        return ordered.stream()
            .filter(e -> e.action() == action)
            .limit(limit)
            .toList();
    }

    @Override
    public List<AuditEvent> findByTimeRange(
        Instant from,
        Instant to,
        int limit
    ) {
        return ordered.stream()
            .filter(e ->
                !e.timestamp().isBefore(from)
                    && !e.timestamp().isAfter(to))
            .limit(limit)
            .toList();
    }

    @Override
    public List<AuditEvent> findByExecution(
        String executionId,
        int limit
    ) {
        return ordered.stream()
            .filter(e -> executionId.equals(
                e.executionId()))
            .limit(limit)
            .toList();
    }

    @Override
    public int purgeOlderThan(Instant cutoff) {
        List<String> toRemove = ordered.stream()
            .filter(e -> e.timestamp().isBefore(cutoff))
            .map(AuditEvent::eventId)
            .toList();

        for (String id : toRemove) {
            events.remove(id);
        }
        ordered.removeIf(e ->
            e.timestamp().isBefore(cutoff));

        return toRemove.size();
    }

    @Override
    public long count() {
        return events.size();
    }

    /**
     * Get all events (for testing).
     */
    public List<AuditEvent> all() {
        return List.copyOf(ordered);
    }
}
