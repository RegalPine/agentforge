package io.github.regalpine.agentforge.runtime;

import io.github.regalpine.agentforge.event.Execution;
import io.github.regalpine.agentforge.event.ExecutionState;
import io.github.regalpine.agentforge.graph.ir.ExecutionPlan;

import java.util.Map;

public record NodeExecutionContext(
    Execution execution,
    ExecutionState state,
    ExecutionPlan plan,
    Map<String, Object> attributes
) {

    public NodeExecutionContext {
        attributes =
            attributes == null
                ? Map.of()
                : Map.copyOf(attributes);
    }
}
