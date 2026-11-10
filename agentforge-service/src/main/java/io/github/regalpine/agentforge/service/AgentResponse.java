package io.github.regalpine.agentforge.service;

import java.util.Map;
import java.util.Objects;

/**
 * Agent response.
 * AF-ENHANCE-11 §11: AgentResponse.
 */
public record AgentResponse(
    String responseId,
    String executionId,
    Object output,
    Map<String, Object> metadata
) {

    public AgentResponse {
        Objects.requireNonNull(
            responseId, "responseId"
        );
        Objects.requireNonNull(
            executionId, "executionId"
        );

        output = output == null
            ? "" : output;
        metadata = metadata == null
            ? Map.of() : Map.copyOf(metadata);
    }

    /**
     * Create a simple response.
     */
    public static AgentResponse of(
        String responseId,
        String executionId,
        Object output
    ) {
        return new AgentResponse(
            responseId, executionId,
            output, Map.of()
        );
    }
}
