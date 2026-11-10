package io.github.regalpine.agentforge.runtime;

import java.time.Instant;

/**
 * Internal node execution record.
 * AF-05 §22: NodeExecution.
 *
 * <p>Tracks individual node execution attempts
 * within a graph execution. A node can have
 * multiple attempts (e.g. retries).
 */
public record NodeExecution(
    String executionId,
    String nodeId,
    long attempt,
    NodeExecutionState state,
    Instant startedAt,
    Instant endedAt
) {
}
