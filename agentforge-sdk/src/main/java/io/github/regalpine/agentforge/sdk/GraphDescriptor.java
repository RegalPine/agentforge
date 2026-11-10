package io.github.regalpine.agentforge.sdk;

import java.util.*;

/**
 * Descriptor for a graph definition.
 * Used by the GraphBuilder to produce
 * a serialisable graph specification.
 */
public record GraphDescriptor(
    String graphId,
    String description,
    List<GraphBuilder.NodeDef> nodes,
    List<GraphBuilder.EdgeDef> edges,
    Map<String, Object> config
) {

    public GraphDescriptor {
        Objects.requireNonNull(graphId);
        description = description == null
            ? "" : description;
        nodes = nodes == null
            ? List.of() : List.copyOf(nodes);
        edges = edges == null
            ? List.of() : List.copyOf(edges);
        config = config == null
            ? Map.of() : Map.copyOf(config);
    }
}
