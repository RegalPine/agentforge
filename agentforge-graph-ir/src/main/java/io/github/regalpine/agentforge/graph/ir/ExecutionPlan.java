package io.github.regalpine.agentforge.graph.ir;

import io.github.regalpine.agentforge.kernel.EntityId;
import io.github.regalpine.agentforge.kernel.Version;

import java.util.List;
import java.util.Map;

public record ExecutionPlan(
    EntityId graphId,
    Version graphVersion,
    String fingerprint,
    EntityId entryNode,
    List<ExecutionNode> nodes,
    Map<EntityId, List<RoutingEntry>> routing,
    Map<String, Object> policies
) {

    public ExecutionPlan {
        nodes =
            nodes == null
                ? List.of()
                : List.copyOf(nodes);

        routing =
            routing == null
                ? Map.of()
                : Map.copyOf(routing);

        policies =
            policies == null
                ? Map.of()
                : Map.copyOf(policies);
    }
}
