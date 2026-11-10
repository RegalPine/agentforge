package io.github.regalpine.agentforge.model;

/**
 * Base interface for all message types in a chat conversation.
 * <p>
 * Messages represent the different roles in a conversation:
 * <ul>
 *   <li>{@link SystemMessage} - System instructions</li>
 *   <li>{@link UserMessage} - User input</li>
 *   <li>{@link AssistantMessage} - Model responses</li>
 *   <li>{@link ToolMessage} - Tool execution results</li>
 * </ul>
 */
public sealed interface Message permits SystemMessage, UserMessage, AssistantMessage, ToolMessage {

    /**
     * Get the role of this message.
     *
     * @return the message role
     */
    MessageRole role();

    /**
     * Get the content of this message.
     *
     * @return the message content, may be empty
     */
    String content();
}
