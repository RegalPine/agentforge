package io.github.regalpine.agentforge.service;

import java.util.Optional;

/**
 * Session store.
 * AF-ENHANCE-11 §29: Session Store.
 */
public interface SessionStore {

    /**
     * Create a new session.
     */
    Session create(
        String sessionId,
        String tenantId
    );

    /**
     * Get a session by ID.
     */
    Optional<Session> get(
        String sessionId
    );

    /**
     * Update a session.
     */
    void update(Session session);

    /**
     * Close a session.
     */
    void close(String sessionId);

    /**
     * Delete a session.
     */
    void delete(String sessionId);
}
