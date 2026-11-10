package io.github.regalpine.agentforge.service;

import java.util.Map;
import java.util.Objects;

/**
 * Agent request.
 * AF-ENHANCE-11 §10: AgentRequest.
 */
public record AgentRequest(
    String requestId,
    Object input,
    String sessionReference,
    String conversationReference,
    String idempotencyKey,
    Map<String, Object> metadata
) {

    public AgentRequest {
        Objects.requireNonNull(
            requestId, "requestId"
        );
        Objects.requireNonNull(
            input, "input"
        );

        sessionReference =
            sessionReference == null
                ? "" : sessionReference;
        conversationReference =
            conversationReference == null
                ? "" : conversationReference;
        idempotencyKey =
            idempotencyKey == null
                ? "" : idempotencyKey;
        metadata = metadata == null
            ? Map.of() : Map.copyOf(metadata);
    }

    /**
     * Create a simple request.
     */
    public static AgentRequest of(
        String requestId,
        Object input
    ) {
        return new AgentRequest(
            requestId, input,
            "", "", "", Map.of()
        );
    }
}
