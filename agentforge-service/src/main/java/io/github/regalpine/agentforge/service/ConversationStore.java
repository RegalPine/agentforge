package io.github.regalpine.agentforge.service;

import java.util.Optional;

/**
 * Conversation store.
 * AF-ENHANCE-11 §30: Conversation Store.
 */
public interface ConversationStore {

    /**
     * Create a new conversation
     * for a session.
     */
    Conversation create(
        String conversationId,
        String sessionId
    );

    /**
     * Get a conversation by ID.
     */
    Optional<Conversation> get(
        String conversationId
    );

    /**
     * Append a message to a
     * conversation.
     */
    void append(
        String conversationId,
        ConversationMessage message
    );
}
