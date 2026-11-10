package io.github.regalpine.agentforge.event;

import io.github.regalpine.agentforge.event.ExecutionId;

import io.github.regalpine.agentforge.kernel.EntityId;

import java.util.Map;
import java.util.Set;

public record ExecutionState(
    ExecutionId executionId,
    ExecutionStatus status,
    EntityId currentNode,
    Map<String, Object> data,
    Set<EntityId> completedNodes,
    Set<EntityId> waitingNodes,
    long version,
    long nextEventSequence
) {

    public ExecutionState {
        data =
            data == null
                ? Map.of()
                : Map.copyOf(data);

        completedNodes =
            completedNodes == null
                ? Set.of()
                : Set.copyOf(completedNodes);

        waitingNodes =
            waitingNodes == null
                ? Set.of()
                : Set.copyOf(waitingNodes);
    }

    /**
     * Backward-compatible constructor.
     * Defaults nextEventSequence to 0.
     */
    public ExecutionState(
        ExecutionId executionId,
        ExecutionStatus status,
        EntityId currentNode,
        Map<String, Object> data,
        Set<EntityId> completedNodes,
        Set<EntityId> waitingNodes,
        long version
    ) {
        this(
            executionId, status, currentNode,
            data, completedNodes, waitingNodes,
            version, 0
        );
    }

    /**
     * Create a copy with an updated nextEventSequence.
     */
    public ExecutionState withNextEventSequence(long seq) {
        return new ExecutionState(
            executionId, status, currentNode,
            data, completedNodes, waitingNodes,
            version, seq
        );
    }
}
