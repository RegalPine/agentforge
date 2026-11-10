package io.github.regalpine.agentforge.sdk.bootstrap;

/**
 * Bootstrap lifecycle states.
 * AF-ENHANCE-01 §19: Bootstrap lifecycle.
 *
 * <pre>
 * CREATE → LOAD → RESOLVE → VALIDATE
 *   → INITIALIZE → READY → RUNNING
 *   → STOPPING → STOPPED
 * </pre>
 *
 * <p>Any state can transition to FAILED
 * if an error occurs.
 */
public enum BootstrapState {

    /** Initial state: bootstrap created. */
    CREATE,

    /** Loading configuration and definitions. */
    LOAD,

    /** Resolving dependencies. */
    RESOLVE,

    /** Validating semantic integrity. */
    VALIDATE,

    /** Initializing runtime components. */
    INITIALIZE,

    /** All components ready, not yet running. */
    READY,

    /** Runtime is actively serving. */
    RUNNING,

    /** Graceful shutdown in progress. */
    STOPPING,

    /** Shutdown complete. */
    STOPPED,

    /** Fatal error during bootstrap. */
    FAILED;

    /**
     * Check if this state allows
     * transition to the target.
     */
    public boolean canTransitionTo(
        BootstrapState target
    ) {
        return switch (this) {
            case CREATE ->
                target == LOAD
                    || target == FAILED;
            case LOAD ->
                target == RESOLVE
                    || target == FAILED;
            case RESOLVE ->
                target == VALIDATE
                    || target == FAILED;
            case VALIDATE ->
                target == INITIALIZE
                    || target == FAILED;
            case INITIALIZE ->
                target == READY
                    || target == FAILED;
            case READY ->
                target == RUNNING
                    || target == STOPPING
                    || target == FAILED;
            case RUNNING ->
                target == STOPPING
                    || target == FAILED;
            case STOPPING ->
                target == STOPPED
                    || target == FAILED;
            case STOPPED -> false;
            case FAILED -> false;
        };
    }

    /**
     * Check if this is a terminal state.
     */
    public boolean isTerminal() {
        return this == STOPPED
            || this == FAILED;
    }

    /**
     * Check if the runtime is active.
     */
    public boolean isActive() {
        return this == RUNNING;
    }

    /**
     * Check if bootstrap is still
     * in progress.
     */
    public boolean isStarting() {
        return this == CREATE
            || this == LOAD
            || this == RESOLVE
            || this == VALIDATE
            || this == INITIALIZE
            || this == READY;
    }
}
