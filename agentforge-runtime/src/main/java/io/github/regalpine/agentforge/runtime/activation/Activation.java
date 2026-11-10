package io.github.regalpine.agentforge.runtime.activation;

import io.github.regalpine.agentforge.kernel.EntityId;
import io.github.regalpine.agentforge.event.ExecutionId;

import java.time.Instant;
import java.util.Map;
import java.util.Set;

/**
 * Represents an execution activation — a fork point in the execution graph.
 * When a PARALLEL or SUBGRAPH node is reached, an Activation is created
 * to track the spawned branches.
 *
 * <p>G-01: Activation/Token/Frame persistence support.
 */
public record Activation(
        String activationId,
        ExecutionId executionId,
        EntityId sourceNode,
        ActivationStatus status,
        Set<EntityId> branches,
        Map<String, Object> data,
        Instant createdAt
) {

    public Activation {
        branches = branches == null ? Set.of() : Set.copyOf(branches);
        data = data == null ? Map.of() : Map.copyOf(data);
    }
}
