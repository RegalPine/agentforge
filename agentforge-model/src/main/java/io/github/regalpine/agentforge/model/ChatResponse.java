package io.github.regalpine.agentforge.model;

import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Response from a chat model.
 * <p>
 * A chat response contains the model's output, including the content,
 * any tool calls, finish reason, token usage, and metadata.
 */
public record ChatResponse(
    String content,
    List<ToolCall> toolCalls,
    FinishReason finishReason,
    TokenUsage usage,
    String model,
    Map<String, Object> metadata
) {

    public ChatResponse {
        Objects.requireNonNull(content, "content");
        Objects.requireNonNull(finishReason, "finishReason");
        Objects.requireNonNull(usage, "usage");

        toolCalls = toolCalls == null ? List.of() : List.copyOf(toolCalls);
        metadata = metadata == null ? Map.of() : Map.copyOf(metadata);
    }

    /**
     * Create a simple chat response with just content.
     *
     * @param content the response content
     * @return a new ChatResponse
     */
    public static ChatResponse of(String content) {
        return new ChatResponse(content, List.of(), FinishReason.STOP, TokenUsage.empty(), null, null);
    }

    /**
     * Create a chat response with content and usage.
     *
     * @param content the response content
     * @param usage   the token usage
     * @return a new ChatResponse
     */
    public static ChatResponse of(String content, TokenUsage usage) {
        return new ChatResponse(content, List.of(), FinishReason.STOP, usage, null, null);
    }

    /**
     * Create a chat response with tool calls.
     *
     * @param toolCalls the tool calls
     * @return a new ChatResponse
     */
    public static ChatResponse withToolCalls(List<ToolCall> toolCalls) {
        return new ChatResponse("", toolCalls, FinishReason.TOOL_CALLS, TokenUsage.empty(), null, null);
    }

    /**
     * Builder for creating ChatResponse instances.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for ChatResponse.
     */
    public static class Builder {
        private String content = "";
        private List<ToolCall> toolCalls = List.of();
        private FinishReason finishReason = FinishReason.STOP;
        private TokenUsage usage = TokenUsage.empty();
        private String model;
        private Map<String, Object> metadata = Map.of();

        public Builder content(String content) {
            this.content = content;
            return this;
        }

        public Builder toolCalls(List<ToolCall> toolCalls) {
            this.toolCalls = toolCalls;
            return this;
        }

        public Builder finishReason(FinishReason finishReason) {
            this.finishReason = finishReason;
            return this;
        }

        public Builder usage(TokenUsage usage) {
            this.usage = usage;
            return this;
        }

        public Builder model(String model) {
            this.model = model;
            return this;
        }

        public Builder metadata(Map<String, Object> metadata) {
            this.metadata = metadata;
            return this;
        }

        public ChatResponse build() {
            return new ChatResponse(content, toolCalls, finishReason, usage, model, metadata);
        }
    }
}
