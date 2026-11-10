package io.github.regalpine.agentforge.graph;

import io.github.regalpine.agentforge.kernel.EntityId;

public record EdgeDefinition(
    EntityId id,
    EntityId source,
    EntityId target,
    EdgeType type,
    String condition
) {

    public EdgeDefinition {
        if (id == null ||
            source == null ||
            target == null) {

            throw new IllegalArgumentException(
                "Edge id/source/target cannot be null"
            );
        }

        if (type == null) {
            type = EdgeType.NORMAL;
        }
    }
}
