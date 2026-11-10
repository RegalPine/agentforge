package io.github.regalpine.agentforge.model;

import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Request for structured output.
 */
public record StructuredChatRequest(
    List<Message> messages,
    OutputSpec outputSpec,
    String systemInstruction,
    Double temperature,
    Integer maxTokens,
    Map<String, Object> metadata
) {

    public StructuredChatRequest {
        Objects.requireNonNull(messages, "messages");
        Objects.requireNonNull(outputSpec, "outputSpec");

        if (messages.isEmpty()) {
            throw new IllegalArgumentException("messages cannot be empty");
        }

        messages = List.copyOf(messages);
        metadata = metadata == null ? Map.of() : Map.copyOf(metadata);
    }

    /**
     * Create a structured chat request.
     *
     * @param messages  the conversation messages
     * @param outputSpec the output specification
     * @return a new StructuredChatRequest
     */
    public static StructuredChatRequest of(List<Message> messages, OutputSpec outputSpec) {
        return new StructuredChatRequest(messages, outputSpec, null, null, null, null);
    }
}
