package io.github.regalpine.agentforge.a2a;

import java.util.Map;
import java.util.Objects;

/**
 * A2A request.
 * AF-ENHANCE-10 §30: A2A Invocation.
 */
public record A2ARequest(
    String requestId,
    A2AMessage message,
    String correlationId,
    String idempotencyKey,
    Map<String, Object> metadata
) {

    public A2ARequest {
        Objects.requireNonNull(
            requestId, "requestId"
        );
        Objects.requireNonNull(
            message, "message"
        );

        correlationId =
            correlationId == null
                ? "" : correlationId;
        idempotencyKey =
            idempotencyKey == null
                ? "" : idempotencyKey;
        metadata = metadata == null
            ? Map.of() : Map.copyOf(metadata);
    }

    /**
     * Create a simple request.
     */
    public static A2ARequest of(
        String requestId,
        A2AMessage message
    ) {
        return new A2ARequest(
            requestId, message,
            null, null, null
        );
    }
}
