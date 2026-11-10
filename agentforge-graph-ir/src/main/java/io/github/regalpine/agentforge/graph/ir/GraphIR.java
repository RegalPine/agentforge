package io.github.regalpine.agentforge.graph.ir;

import io.github.regalpine.agentforge.kernel.EntityId;
import io.github.regalpine.agentforge.kernel.Version;

import java.util.List;
import java.util.Map;

public record GraphIR(
    EntityId graphId,
    Version graphVersion,
    String compilerVersion,
    List<IRNode> nodes,
    List<IREdge> edges,
    Map<String, Object> resolvedReferences,
    Map<String, Object> metadata
) {

    public GraphIR {
        nodes =
            nodes == null
                ? List.of()
                : List.copyOf(nodes);

        edges =
            edges == null
                ? List.of()
                : List.copyOf(edges);

        resolvedReferences =
            resolvedReferences == null
                ? Map.of()
                : Map.copyOf(resolvedReferences);

        metadata =
            metadata == null
                ? Map.of()
                : Map.copyOf(metadata);
    }
}
