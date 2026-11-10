package io.github.regalpine.agentforge.graph.ir;

import io.github.regalpine.agentforge.graph.EdgeType;
import io.github.regalpine.agentforge.kernel.EntityId;

public record IREdge(
    EntityId id,
    EntityId source,
    EntityId target,
    EdgeType type,
    String condition
) {
}
