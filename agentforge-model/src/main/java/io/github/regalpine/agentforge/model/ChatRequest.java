package io.github.regalpine.agentforge.model;

import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Request to a chat model.
 * <p>
 * A chat request contains the messages to send to the model, along with
 * various parameters that control the model's behavior.
 * <p>
 * Provider-specific parameters should be placed in {@code providerOptions}
 * rather than polluting the core model contract.
 */
public record ChatRequest(
    List<Message> messages,
    String systemInstruction,
    Double temperature,
    Integer maxTokens,
    List<ToolDescriptor> tools,
    ResponseFormat responseFormat,
    Map<String, Object> providerOptions,
    Map<String, Object> metadata
) {

    public ChatRequest {
        Objects.requireNonNull(messages, "messages");

        if (messages.isEmpty()) {
            throw new IllegalArgumentException("messages cannot be empty");
        }

        messages = List.copyOf(messages);
        tools = tools == null ? List.of() : List.copyOf(tools);
        providerOptions = providerOptions == null ? Map.of() : Map.copyOf(providerOptions);
        metadata = metadata == null ? Map.of() : Map.copyOf(metadata);
    }

    /**
     * Create a simple chat request with just messages.
     *
     * @param messages the conversation messages
     * @return a new ChatRequest
     */
    public static ChatRequest of(List<Message> messages) {
        return new ChatRequest(messages, null, null, null, null, null, null, null);
    }

    /**
     * Create a chat request with messages and tools.
     *
     * @param messages the conversation messages
     * @param tools    the available tools
     * @return a new ChatRequest
     */
    public static ChatRequest of(List<Message> messages, List<ToolDescriptor> tools) {
        return new ChatRequest(messages, null, null, null, tools, null, null, null);
    }

    /**
     * Builder for creating ChatRequest instances.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for ChatRequest.
     */
    public static class Builder {
        private List<Message> messages;
        private String systemInstruction;
        private Double temperature;
        private Integer maxTokens;
        private List<ToolDescriptor> tools;
        private ResponseFormat responseFormat;
        private Map<String, Object> providerOptions;
        private Map<String, Object> metadata;

        public Builder messages(List<Message> messages) {
            this.messages = messages;
            return this;
        }

        public Builder systemInstruction(String systemInstruction) {
            this.systemInstruction = systemInstruction;
            return this;
        }

        public Builder temperature(Double temperature) {
            this.temperature = temperature;
            return this;
        }

        public Builder maxTokens(Integer maxTokens) {
            this.maxTokens = maxTokens;
            return this;
        }

        public Builder tools(List<ToolDescriptor> tools) {
            this.tools = tools;
            return this;
        }

        public Builder responseFormat(ResponseFormat responseFormat) {
            this.responseFormat = responseFormat;
            return this;
        }

        public Builder providerOptions(Map<String, Object> providerOptions) {
            this.providerOptions = providerOptions;
            return this;
        }

        public Builder metadata(Map<String, Object> metadata) {
            this.metadata = metadata;
            return this;
        }

        public ChatRequest build() {
            return new ChatRequest(messages, systemInstruction, temperature, maxTokens, tools, responseFormat, providerOptions, metadata);
        }
    }
}
