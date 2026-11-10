package io.github.regalpine.agentforge.model;

import java.util.Objects;

/**
 * User message containing user input.
 */
public record UserMessage(String content) implements Message {

    public UserMessage {
        Objects.requireNonNull(content, "content");
    }

    public static UserMessage of(String content) {
        return new UserMessage(content);
    }

    @Override
    public MessageRole role() {
        return MessageRole.USER;
    }
}
