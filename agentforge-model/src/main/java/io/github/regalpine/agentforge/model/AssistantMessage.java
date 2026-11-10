package io.github.regalpine.agentforge.model;

import java.util.List;
import java.util.Objects;

/**
 * Assistant message containing model response.
 */
public record AssistantMessage(
    String content,
    List<ToolCall> toolCalls
) implements Message {

    public AssistantMessage {
        Objects.requireNonNull(content, "content");
        toolCalls = toolCalls == null ? List.of() : List.copyOf(toolCalls);
    }

    public static AssistantMessage of(String content) {
        return new AssistantMessage(content, List.of());
    }

    public static AssistantMessage of(String content, List<ToolCall> toolCalls) {
        return new AssistantMessage(content, toolCalls);
    }

    @Override
    public MessageRole role() {
        return MessageRole.ASSISTANT;
    }
}
