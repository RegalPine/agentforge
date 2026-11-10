package io.github.regalpine.agentforge.service;

import java.time.Instant;
import java.util.Map;
import java.util.Objects;

/**
 * Session — interaction container.
 * AF-ENHANCE-11 §20-23: Session.
 *
 * <p>Session ≠ Execution.
 * Session is a persistent
 * interaction container that
 * can hold multiple Executions.
 */
public record Session(
    String sessionId,
    String tenantId,
    String actorId,
    SessionStatus status,
    Instant createdAt,
    Instant lastActivityAt,
    Instant expiresAt,
    Map<String, Object> context
) {

    public Session {
        Objects.requireNonNull(
            sessionId, "sessionId"
        );
        Objects.requireNonNull(
            tenantId, "tenantId"
        );
        Objects.requireNonNull(
            status, "status"
        );

        actorId = actorId == null
            ? "" : actorId;
        createdAt = createdAt == null
            ? Instant.now() : createdAt;
        lastActivityAt = lastActivityAt == null
            ? createdAt : lastActivityAt;
        context = context == null
            ? Map.of() : Map.copyOf(context);
    }

    /**
     * Create a new session.
     */
    public static Session create(
        String sessionId,
        String tenantId
    ) {
        return new Session(
            sessionId, tenantId, "",
            SessionStatus.CREATED,
            Instant.now(), Instant.now(),
            null, Map.of()
        );
    }

    /**
     * Activate the session.
     */
    public Session activate() {
        return new Session(
            sessionId, tenantId, actorId,
            SessionStatus.ACTIVE,
            createdAt, Instant.now(),
            expiresAt, context
        );
    }

    /**
     * Close the session.
     */
    public Session close() {
        return new Session(
            sessionId, tenantId, actorId,
            SessionStatus.CLOSED,
            createdAt, lastActivityAt,
            expiresAt, context
        );
    }
}
