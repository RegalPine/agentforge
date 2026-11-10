package io.github.regalpine.agentforge.multiagent.invocation;

/**
 * InvocationStatus — AF-ENHANCE-08 §8.
 *
 * Lifecycle of an agent invocation:
 *
 * <p>Normal flow:
 * REQUESTED → AUTHORIZED → STARTED
 * → RUNNING → COMPLETED
 *
 * <p>Abnormal:
 * REJECTED, FAILED, CANCELLED, TIMEOUT
 */
public enum InvocationStatus {

    /**
     * Invocation requested.
     */
    REQUESTED,

    /**
     * Authorization granted.
     */
    AUTHORIZED,

    /**
     * Target execution started.
     */
    STARTED,

    /**
     * Target execution running.
     */
    RUNNING,

    /**
     * Completed successfully.
     */
    COMPLETED,

    /**
     * Rejected by policy.
     */
    REJECTED,

    /**
     * Failed during execution.
     */
    FAILED,

    /**
     * Cancelled by caller or runtime.
     */
    CANCELLED,

    /**
     * Timed out.
     */
    TIMEOUT;

    /**
     * Check if this is a terminal state.
     */
    public boolean isTerminal() {
        return this == COMPLETED
            || this == REJECTED
            || this == FAILED
            || this == CANCELLED
            || this == TIMEOUT;
    }

    /**
     * Check if this is an active state.
     */
    public boolean isActive() {
        return this == STARTED
            || this == RUNNING;
    }

    /**
     * Check if this is a success state.
     */
    public boolean isSuccess() {
        return this == COMPLETED;
    }

    /**
     * Check if transition is valid.
     */
    public boolean canTransitionTo(
        InvocationStatus target
    ) {
        return switch (this) {
            case REQUESTED ->
                target == AUTHORIZED
                    || target == REJECTED
                    || target == CANCELLED;
            case AUTHORIZED ->
                target == STARTED
                    || target == CANCELLED
                    || target == FAILED;
            case STARTED ->
                target == RUNNING
                    || target == COMPLETED
                    || target == FAILED
                    || target == CANCELLED
                    || target == TIMEOUT;
            case RUNNING ->
                target == COMPLETED
                    || target == FAILED
                    || target == CANCELLED
                    || target == TIMEOUT;
            default -> false;
        };
    }
}
