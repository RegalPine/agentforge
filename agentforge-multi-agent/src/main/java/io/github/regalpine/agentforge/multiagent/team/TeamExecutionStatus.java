package io.github.regalpine.agentforge.multiagent.team;

/**
 * TeamExecutionStatus —
 * AF-ENHANCE-08 §56.
 *
 * Lifecycle of a team execution:
 *
 * <p>Normal:
 * CREATED → PLANNED → RUNNING
 * → PARTIAL → COMPLETED
 *
 * <p>Abnormal:
 * FAILED, CANCELLED, TIMEOUT,
 * ABORTED
 */
public enum TeamExecutionStatus {

    /**
     * Team execution created.
     */
    CREATED,

    /**
     * Execution plan created.
     */
    PLANNED,

    /**
     * Currently running.
     */
    RUNNING,

    /**
     * Partial completion.
     * Some agents done, some not.
     */
    PARTIAL,

    /**
     * All agents completed.
     */
    COMPLETED,

    /**
     * Failed.
     */
    FAILED,

    /**
     * Cancelled.
     */
    CANCELLED,

    /**
     * Timed out.
     */
    TIMEOUT,

    /**
     * Aborted.
     */
    ABORTED;

    /**
     * Check if terminal.
     */
    public boolean isTerminal() {
        return this == COMPLETED
            || this == FAILED
            || this == CANCELLED
            || this == TIMEOUT
            || this == ABORTED;
    }

    /**
     * Check if active.
     */
    public boolean isActive() {
        return this == RUNNING
            || this == PLANNED;
    }
}
