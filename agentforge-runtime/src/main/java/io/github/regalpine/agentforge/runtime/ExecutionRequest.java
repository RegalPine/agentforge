package io.github.regalpine.agentforge.runtime;

import io.github.regalpine.agentforge.kernel.EntityId;
import io.github.regalpine.agentforge.kernel.Version;

import java.util.Map;

public record ExecutionRequest(
    EntityId graphId,
    Version graphVersion,
    Map<String, Object> inputs,
    String tenantId
) {

    public ExecutionRequest {
        if (graphId == null) {
            throw new IllegalArgumentException(
                "graphId cannot be null"
            );
        }

        if (graphVersion == null) {
            throw new IllegalArgumentException(
                "graphVersion cannot be null"
            );
        }

        inputs =
            inputs == null
                ? Map.of()
                : Map.copyOf(inputs);
    }
}
