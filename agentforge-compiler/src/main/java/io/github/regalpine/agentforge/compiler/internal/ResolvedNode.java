package io.github.regalpine.agentforge.compiler.internal;

import io.github.regalpine.agentforge.graph.NodeDefinition;

import java.util.Map;

public record ResolvedNode(
    NodeDefinition definition,
    Map<String, Object> resolvedReferences
) {

    public ResolvedNode {
        resolvedReferences =
            resolvedReferences == null
                ? Map.of()
                : Map.copyOf(resolvedReferences);
    }
}
