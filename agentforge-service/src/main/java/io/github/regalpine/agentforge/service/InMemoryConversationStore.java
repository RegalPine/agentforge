package io.github.regalpine.agentforge.service;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory conversation store.
 * AF-ENHANCE-11 §30.
 */
public final class InMemoryConversationStore
    implements ConversationStore {

    private final Map<String, Conversation>
        conversations = new ConcurrentHashMap<>();

    @Override
    public Conversation create(
        String conversationId,
        String sessionId
    ) {
        if (conversationId == null) {
            throw new IllegalArgumentException(
                "conversationId must not be null"
            );
        }

        var conversation =
            Conversation.create(
                conversationId, sessionId
            );

        conversations.put(
            conversationId,
            conversation
        );

        return conversation;
    }

    @Override
    public Optional<Conversation> get(
        String conversationId
    ) {
        if (conversationId == null) {
            return Optional.empty();
        }

        return Optional.ofNullable(
            conversations.get(conversationId)
        );
    }

    @Override
    public void append(
        String conversationId,
        ConversationMessage message
    ) {
        var conversation =
            conversations.get(
                conversationId
            );

        if (conversation != null) {
            conversations.put(
                conversationId,
                conversation.append(message)
            );
        }
    }
}
