package io.github.regalpine.agentforge.service;

import java.time.Instant;
import java.util.Map;
import java.util.Objects;

/**
 * Conversation message.
 * AF-ENHANCE-11 §25: Message.
 */
public record ConversationMessage(
    String messageId,
    MessageRole role,
    Object content,
    Instant timestamp,
    Map<String, Object> metadata
) {

    public ConversationMessage {
        Objects.requireNonNull(
            messageId, "messageId"
        );
        Objects.requireNonNull(
            role, "role"
        );
        Objects.requireNonNull(
            content, "content"
        );

        timestamp = timestamp == null
            ? Instant.now() : timestamp;
        metadata = metadata == null
            ? Map.of() : Map.copyOf(metadata);
    }

    /**
     * Create a user message.
     */
    public static ConversationMessage user(
        String messageId,
        Object content
    ) {
        return new ConversationMessage(
            messageId, MessageRole.USER,
            content, Instant.now(), Map.of()
        );
    }

    /**
     * Create an assistant message.
     */
    public static ConversationMessage assistant(
        String messageId,
        Object content
    ) {
        return new ConversationMessage(
            messageId,
            MessageRole.ASSISTANT,
            content, Instant.now(), Map.of()
        );
    }
}
