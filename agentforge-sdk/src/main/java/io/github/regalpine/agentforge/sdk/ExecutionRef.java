package io.github.regalpine.agentforge.sdk;

/**
 * Reference to an active execution.
 * <p>
 * Provides access to execution status, result, and state.
 * Also supports cancellation.
 * <p>
 * Thread-safe.
 */
public interface ExecutionRef {

    /**
     * The unique execution identifier.
     */
    String executionId();

    /**
     * The agent that is executing (if applicable).
     */
    String agentId();

    /**
     * The graph that is executing (if applicable).
     */
    String graphId();

    /**
     * Get the current execution status.
     */
    ExecutionStatus status();

    /**
     * Get the execution result. Only available after completion.
     *
     * @return the result, or null if not yet completed
     */
    ExecutionResult result();

    /**
     * Get a read-only view of the current execution state.
     */
    StateView state();

    /**
     * Cancel the execution.
     */
    void cancel();

    /**
     * Check if the execution is still running.
     */
    default boolean isRunning() {
        ExecutionStatus s = status();
        return s == ExecutionStatus.RUNNING || s == ExecutionStatus.WAITING;
    }

    /**
     * Check if the execution has completed (successfully or not).
     */
    default boolean isTerminal() {
        ExecutionStatus s = status();
        return s == ExecutionStatus.COMPLETED
            || s == ExecutionStatus.FAILED
            || s == ExecutionStatus.CANCELLED
            || s == ExecutionStatus.TIMEOUT
            || s == ExecutionStatus.POLICY_DENIED
            || s == ExecutionStatus.BUDGET_EXCEEDED;
    }
}
