package io.github.regalpine.agentforge.model;

import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Request to an embedding model.
 */
public record EmbeddingRequest(
    List<String> texts,
    String model,
    Map<String, Object> metadata
) {

    public EmbeddingRequest {
        Objects.requireNonNull(texts, "texts");

        if (texts.isEmpty()) {
            throw new IllegalArgumentException("texts cannot be empty");
        }

        texts = List.copyOf(texts);
        metadata = metadata == null ? Map.of() : Map.copyOf(metadata);
    }

    /**
     * Create an embedding request for a single text.
     *
     * @param text the text to embed
     * @return a new EmbeddingRequest
     */
    public static EmbeddingRequest of(String text) {
        return new EmbeddingRequest(List.of(text), null, null);
    }

    /**
     * Create an embedding request for multiple texts.
     *
     * @param texts the texts to embed
     * @return a new EmbeddingRequest
     */
    public static EmbeddingRequest of(List<String> texts) {
        return new EmbeddingRequest(texts, null, null);
    }
}
