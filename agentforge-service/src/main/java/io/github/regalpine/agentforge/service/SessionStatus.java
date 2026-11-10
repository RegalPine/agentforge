package io.github.regalpine.agentforge.service;

/**
 * Session lifecycle states.
 * AF-ENHANCE-11 §22: Session Lifecycle.
 */
public enum SessionStatus {

    CREATED,

    ACTIVE,

    IDLE,

    EXPIRED,

    CLOSED,

    INVALID,

    REVOKED;

    /**
     * Check if the session is active.
     */
    public boolean isActive() {
        return this == ACTIVE
            || this == CREATED;
    }

    /**
     * Check if the session is in a
     * terminal state.
     */
    public boolean isTerminal() {
        return this == EXPIRED
            || this == CLOSED
            || this == INVALID
            || this == REVOKED;
    }
}
