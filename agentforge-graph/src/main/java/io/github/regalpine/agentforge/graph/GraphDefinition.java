package io.github.regalpine.agentforge.graph;

import io.github.regalpine.agentforge.kernel.Entity;
import io.github.regalpine.agentforge.kernel.EntityId;
import io.github.regalpine.agentforge.kernel.Version;

import java.util.List;
import java.util.Map;

public record GraphDefinition(
    EntityId id,
    Version version,
    String name,
    String description,
    Map<String, Object> inputs,
    Map<String, Object> outputs,
    List<NodeDefinition> nodes,
    List<EdgeDefinition> edges,
    List<DataBinding> bindings,
    List<Expression> expressions,
    Map<String, Object> policies,
    Map<String, Object> metadata
) implements Entity {

    public GraphDefinition {

        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException(
                "Graph name cannot be blank"
            );
        }

        inputs =
            inputs == null
                ? Map.of()
                : Map.copyOf(inputs);

        outputs =
            outputs == null
                ? Map.of()
                : Map.copyOf(outputs);

        nodes =
            nodes == null
                ? List.of()
                : List.copyOf(nodes);

        edges =
            edges == null
                ? List.of()
                : List.copyOf(edges);

        bindings =
            bindings == null
                ? List.of()
                : List.copyOf(bindings);

        expressions =
            expressions == null
                ? List.of()
                : List.copyOf(expressions);

        policies =
            policies == null
                ? Map.of()
                : Map.copyOf(policies);

        metadata =
            metadata == null
                ? Map.of()
                : Map.copyOf(metadata);
    }
}
