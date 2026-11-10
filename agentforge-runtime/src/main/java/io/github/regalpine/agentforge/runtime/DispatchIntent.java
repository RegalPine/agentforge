package io.github.regalpine.agentforge.runtime;

import io.github.regalpine.agentforge.kernel.EntityId;

import java.util.Map;
import java.util.Objects;

/** 派发意图：提交事务内记录的后续任务派发声明。 */
public record DispatchIntent(
    String dispatchId,
    EntityId targetNodeId,
    String activationId,
    int generation,
    Map<String, Object> input
) {
    public DispatchIntent {
        Objects.requireNonNull(dispatchId);
        Objects.requireNonNull(targetNodeId);
        Objects.requireNonNull(activationId);
        input = input == null ? Map.of() : Map.copyOf(input);
    }
}
