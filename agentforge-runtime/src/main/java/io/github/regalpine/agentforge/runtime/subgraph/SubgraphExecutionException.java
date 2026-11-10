package io.github.regalpine.agentforge.runtime.subgraph;

import io.github.regalpine.agentforge.event.ExecutionId;

/**
 * Exception thrown when a subgraph execution fails.
 */
public class SubgraphExecutionException extends RuntimeException {

    private final ExecutionId childExecutionId;

    public SubgraphExecutionException(String message, ExecutionId childExecutionId) {
        super(message);
        this.childExecutionId = childExecutionId;
    }

    public ExecutionId childExecutionId() {
        return childExecutionId;
    }
}
