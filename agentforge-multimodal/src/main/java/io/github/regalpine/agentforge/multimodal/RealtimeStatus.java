package io.github.regalpine.agentforge.multimodal;

/**
 * Realtime session lifecycle.
 * AF-ENHANCE-13 §31: Lifecycle.
 */
public enum RealtimeStatus {

    CREATED,

    CONNECTING,

    CONNECTED,

    ACTIVE,

    IDLE,

    CLOSING,

    CLOSED,

    FAILED,

    DISCONNECTED,

    EXPIRED;

    /**
     * Check if the session is
     * connected and active.
     */
    public boolean isConnected() {
        return this == CONNECTED
            || this == ACTIVE;
    }

    /**
     * Check if the session is
     * in a terminal state.
     */
    public boolean isTerminal() {
        return this == CLOSED
            || this == FAILED
            || this == EXPIRED;
    }
}
