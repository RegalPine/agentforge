package io.github.regalpine.agentforge.sdk;

/**
 * Execution lifecycle status.
 * <p>
 * Mirrors the Runtime execution lifecycle — SDK does not redefine these semantics.
 */
public enum ExecutionStatus {
    CREATED,
    READY,
    RUNNING,
    WAITING,
    COMPLETED,
    FAILED,
    TIMEOUT,
    CANCELLED,
    POLICY_DENIED,
    BUDGET_EXCEEDED
}
