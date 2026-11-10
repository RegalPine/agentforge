package io.github.regalpine.agentforge.model;

import java.util.Map;
import java.util.Objects;

/**
 * Response from structured output.
 */
public record StructuredChatResponse(
    String rawContent,
    Object parsedValue,
    OutputSpec outputSpec,
    TokenUsage usage,
    String model,
    Map<String, Object> metadata
) {

    public StructuredChatResponse {
        Objects.requireNonNull(rawContent, "rawContent");
        Objects.requireNonNull(parsedValue, "parsedValue");
        Objects.requireNonNull(outputSpec, "outputSpec");
        Objects.requireNonNull(usage, "usage");

        metadata = metadata == null ? Map.of() : Map.copyOf(metadata);
    }

    /**
     * Create a structured chat response.
     *
     * @param rawContent the raw content from the model
     * @param parsedValue the parsed and validated value
     * @param outputSpec the output specification
     * @param usage the token usage
     * @return a new StructuredChatResponse
     */
    public static StructuredChatResponse of(
        String rawContent,
        Object parsedValue,
        OutputSpec outputSpec,
        TokenUsage usage
    ) {
        return new StructuredChatResponse(rawContent, parsedValue, outputSpec, usage, null, null);
    }

    /**
     * Get the parsed value as a specific type.
     *
     * @param type the expected type
     * @param <T> the type parameter
     * @return the parsed value cast to the expected type
     * @throws ClassCastException if the value is not of the expected type
     */
    @SuppressWarnings("unchecked")
    public <T> T asTyped(Class<T> type) {
        return type.cast(parsedValue);
    }
}
