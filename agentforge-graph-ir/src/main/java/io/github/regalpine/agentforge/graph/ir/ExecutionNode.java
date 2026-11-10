package io.github.regalpine.agentforge.graph.ir;

import io.github.regalpine.agentforge.graph.NodeType;
import io.github.regalpine.agentforge.kernel.EntityId;

import java.util.Map;

public record ExecutionNode(
    EntityId id,
    NodeType type,
    Map<String, Object> config,
    Map<String, Object> resolvedReferences
) {

    public ExecutionNode {
        config =
            config == null
                ? Map.of()
                : Map.copyOf(config);

        resolvedReferences =
            resolvedReferences == null
                ? Map.of()
                : Map.copyOf(resolvedReferences);
    }
}
