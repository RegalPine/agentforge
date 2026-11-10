package io.github.regalpine.agentforge.a2a;

import java.util.Map;
import java.util.Objects;

/**
 * A2A message.
 * AF-ENHANCE-10 §36: Message.
 *
 * <p>Must be mapped to AgentForge
 * Message: A2AMessage → AgentMessage.
 */
public record A2AMessage(
    String messageId,
    String type,
    Object payload,
    String correlationId,
    String idempotencyKey,
    Map<String, Object> metadata
) {

    public A2AMessage {
        Objects.requireNonNull(
            messageId, "messageId"
        );
        Objects.requireNonNull(
            type, "type"
        );
        Objects.requireNonNull(
            payload, "payload"
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
     * Create a simple text message.
     */
    public static A2AMessage text(
        String messageId,
        String text
    ) {
        return new A2AMessage(
            messageId, "text", text,
            null, null, null
        );
    }

    /**
     * Create a structured message.
     */
    public static A2AMessage structured(
        String messageId,
        Map<String, Object> data
    ) {
        return new A2AMessage(
            messageId, "structured",
            data, null, null, null
        );
    }
}
