package io.github.regalpine.agentforge.service;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory session store.
 * AF-ENHANCE-11 §29.
 */
public final class InMemorySessionStore
    implements SessionStore {

    private final Map<String, Session>
        sessions = new ConcurrentHashMap<>();

    @Override
    public Session create(
        String sessionId,
        String tenantId
    ) {
        if (sessionId == null) {
            throw new IllegalArgumentException(
                "sessionId must not be null"
            );
        }
        if (tenantId == null) {
            throw new IllegalArgumentException(
                "tenantId must not be null"
            );
        }

        var session = Session.create(
            sessionId, tenantId
        );

        sessions.put(sessionId, session);

        return session;
    }

    @Override
    public Optional<Session> get(
        String sessionId
    ) {
        if (sessionId == null) {
            return Optional.empty();
        }

        return Optional.ofNullable(
            sessions.get(sessionId)
        );
    }

    @Override
    public void update(
        Session session
    ) {
        if (session == null) {
            throw new IllegalArgumentException(
                "session must not be null"
            );
        }

        sessions.put(
            session.sessionId(),
            session
        );
    }

    @Override
    public void close(
        String sessionId
    ) {
        var session = sessions.get(sessionId);

        if (session != null) {
            sessions.put(
                sessionId,
                session.close()
            );
        }
    }

    @Override
    public void delete(
        String sessionId
    ) {
        sessions.remove(sessionId);
    }
}
