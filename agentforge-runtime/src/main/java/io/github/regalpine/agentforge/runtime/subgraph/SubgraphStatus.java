package io.github.regalpine.agentforge.runtime.subgraph;

/**
 * Status of a subgraph execution.
 */
public enum SubgraphStatus {
    /** Subgraph execution has been submitted */
    SUBMITTED,
    /** Subgraph is currently executing */
    RUNNING,
    /** Subgraph completed successfully */
    COMPLETED,
    /** Subgraph execution failed */
    FAILED,
    /** Subgraph execution was cancelled */
    CANCELLED
}
