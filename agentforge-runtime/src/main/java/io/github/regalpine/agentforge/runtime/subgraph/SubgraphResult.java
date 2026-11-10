package io.github.regalpine.agentforge.runtime.subgraph;

import io.github.regalpine.agentforge.kernel.EntityId;
import io.github.regalpine.agentforge.event.ExecutionId;

import java.time.Instant;
import java.util.Map;

/**
 * Result of a subgraph execution.
 */
public record SubgraphResult(
        ExecutionId childExecutionId,
        EntityId subgraphNodeId,
        SubgraphStatus status,
        Map<String, Object> outputs,
        Instant completedAt
) {

    public SubgraphResult {
        outputs = outputs == null ? Map.of() : Map.copyOf(outputs);
    }
}
