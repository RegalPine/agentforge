package io.github.regalpine.agentforge.service;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Conversation — message history.
 * AF-ENHANCE-11 §24: Conversation.
 *
 * <p>Conversation ≠ Memory.
 * Conversation is interaction
 * history that can be summarized
 * by Memory Runtime.
 */
public record Conversation(
    String conversationId,
    String sessionId,
    List<ConversationMessage> messages,
    Instant createdAt,
    Instant updatedAt,
    Map<String, Object> metadata
) {

    public Conversation {
        Objects.requireNonNull(
            conversationId,
            "conversationId"
        );
        Objects.requireNonNull(
            sessionId, "sessionId"
        );

        messages = messages == null
            ? List.of() : List.copyOf(messages);
        createdAt = createdAt == null
            ? Instant.now() : createdAt;
        updatedAt = updatedAt == null
            ? createdAt : updatedAt;
        metadata = metadata == null
            ? Map.of() : Map.copyOf(metadata);
    }

    /**
     * Create a new conversation.
     */
    public static Conversation create(
        String conversationId,
        String sessionId
    ) {
        return new Conversation(
            conversationId, sessionId,
            List.of(), Instant.now(),
            Instant.now(), Map.of()
        );
    }

    /**
     * Append a message.
     */
    public Conversation append(
        ConversationMessage message
    ) {
        var newMessages = new java.util.ArrayList<>(
            messages
        );
        newMessages.add(message);

        return new Conversation(
            conversationId, sessionId,
            List.copyOf(newMessages),
            createdAt, Instant.now(),
            metadata
        );
    }
}
