package io.github.regalpine.agentforge.multiagent.messages;

import java.time.Instant;
import java.util.Map;
import java.util.Objects;

/**
 * AgentMessage — AF-ENHANCE-08 §32.
 *
 * Agent-to-agent communication
 * message. Must carry:
 * MessageId, CorrelationId,
 * IdempotencyKey.
 *
 * <p>Agent-to-Agent uses
 * Agent Invocation, not Tool Call.
 */
public record AgentMessage(

    MessageId id,

    String senderAgentId,

    String receiverAgentId,

    String type,

    Object payload,

    String correlationId,

    String idempotencyKey,

    MessageDelivery delivery,

    int sequenceNumber,

    Instant createdAt,

    Map<String, Object> metadata

) {

    public AgentMessage {
        Objects.requireNonNull(
            id, "id"
        );
        Objects.requireNonNull(
            senderAgentId, "senderAgentId"
        );
        Objects.requireNonNull(
            receiverAgentId, "receiverAgentId"
        );
        Objects.requireNonNull(
            type, "type"
        );

        delivery = delivery == null
            ? MessageDelivery.AT_LEAST_ONCE
            : delivery;

        createdAt = createdAt == null
            ? Instant.now() : createdAt;

        metadata = metadata == null
            ? Map.of() : Map.copyOf(metadata);
    }

    /**
     * Create a simple message.
     */
    public static AgentMessage of(
        String sender,
        String receiver,
        String type,
        Object payload
    ) {
        return new AgentMessage(
            MessageId.generate(),
            sender, receiver, type,
            payload, null, null,
            null, 0, null, null
        );
    }

    /**
     * Create with correlation.
     */
    public static AgentMessage of(
        String sender,
        String receiver,
        String type,
        Object payload,
        String correlationId
    ) {
        return new AgentMessage(
            MessageId.generate(),
            sender, receiver, type,
            payload, correlationId,
            null, null, 0, null, null
        );
    }

    /**
     * Create a reply message.
     */
    public AgentMessage reply(
        String type,
        Object payload
    ) {
        return new AgentMessage(
            MessageId.generate(),
            receiverAgentId,
            senderAgentId,
            type, payload,
            id.value(),
            null, null, 0,
            null, null
        );
    }
}
