package io.github.regalpine.agentforge.model;

import java.util.Objects;

/**
 * System message containing instructions for the model.
 */
public record SystemMessage(String content) implements Message {

    public SystemMessage {
        Objects.requireNonNull(content, "content");
    }

    public static SystemMessage of(String content) {
        return new SystemMessage(content);
    }

    @Override
    public MessageRole role() {
        return MessageRole.SYSTEM;
    }
}
