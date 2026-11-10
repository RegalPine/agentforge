package io.github.regalpine.agentforge.controlplane;

/**
 * Runtime instance lifecycle states.
 * AF-ENHANCE-14 §12: Instance Lifecycle.
 */
public enum InstanceStatus {

    CREATED,

    STARTING,

    READY,

    RUNNING,

    DRAINING,

    STOPPED,

    FAILED,

    UNHEALTHY,

    UNKNOWN;

    /**
     * Check if instance can serve.
     */
    public boolean isAvailable() {
        return this == READY
            || this == RUNNING;
    }

    /**
     * Check if instance is in a
     * terminal state.
     */
    public boolean isTerminal() {
        return this == STOPPED
            || this == FAILED;
    }

    /**
     * Check if instance is draining.
     */
    public boolean isDraining() {
        return this == DRAINING;
    }
}
