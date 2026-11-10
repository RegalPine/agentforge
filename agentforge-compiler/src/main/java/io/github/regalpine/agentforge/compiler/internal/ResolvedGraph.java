package io.github.regalpine.agentforge.compiler.internal;

import io.github.regalpine.agentforge.graph.EdgeDefinition;
import io.github.regalpine.agentforge.graph.GraphDefinition;

import java.util.List;

public record ResolvedGraph(
    GraphDefinition graph,
    List<ResolvedNode> nodes,
    List<EdgeDefinition> edges
) {

    public ResolvedGraph {
        nodes =
            nodes == null
                ? List.of()
                : List.copyOf(nodes);

        edges =
            edges == null
                ? List.of()
                : List.copyOf(edges);
    }
}
