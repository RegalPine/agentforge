package io.github.regalpine.agentforge.model;

import java.util.Objects;

/**
 * Tool message containing tool execution results.
 */
public record ToolMessage(
    String callId,
    String content,
    ToolResultStatus status
) implements Message {

    public ToolMessage {
        Objects.requireNonNull(callId, "callId");
        Objects.requireNonNull(content, "content");
        Objects.requireNonNull(status, "status");
    }

    public static ToolMessage success(String callId, String content) {
        return new ToolMessage(callId, content, ToolResultStatus.SUCCESS);
    }

    public static ToolMessage failure(String callId, String error) {
        return new ToolMessage(callId, error, ToolResultStatus.FAILED);
    }

    @Override
    public MessageRole role() {
        return MessageRole.TOOL;
    }
}
