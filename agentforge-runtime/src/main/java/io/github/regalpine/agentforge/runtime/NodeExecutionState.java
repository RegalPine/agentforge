package io.github.regalpine.agentforge.runtime;

/**
 * Node execution state enum (7 states).
 * AF-05 §23: NodeExecutionState.
 *
 * <p>SKIPPED indicates a node that was not executed
 * due to routing or conditional logic.
 */
public enum NodeExecutionState {

    READY,
    RUNNING,
    WAITING,
    COMPLETED,
    FAILED,
    CANCELLED,
    SKIPPED
}
