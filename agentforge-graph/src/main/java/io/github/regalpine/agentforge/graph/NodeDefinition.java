package io.github.regalpine.agentforge.graph;

import io.github.regalpine.agentforge.kernel.EntityId;
import io.github.regalpine.agentforge.kernel.Reference;

import java.util.Map;

public record NodeDefinition(
    EntityId id,
    NodeType type,
    Reference reference,
    Map<String, Object> config
) {

    public NodeDefinition {
        if (id == null) {
            throw new IllegalArgumentException(
                "Node id cannot be null"
            );
        }

        if (type == null) {
            throw new IllegalArgumentException(
                "Node type cannot be null"
            );
        }

        config =
            config == null
                ? Map.of()
                : Map.copyOf(config);
    }
}
