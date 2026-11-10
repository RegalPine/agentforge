package io.github.regalpine.agentforge.model;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Response from an embedding model.
 */
public record EmbeddingResponse(
    List<EmbeddingVector> embeddings,
    TokenUsage usage,
    String model,
    Map<String, Object> metadata
) {

    public EmbeddingResponse {
        Objects.requireNonNull(embeddings, "embeddings");
        Objects.requireNonNull(usage, "usage");

        embeddings = List.copyOf(embeddings);
        metadata = metadata == null ? Map.of() : Map.copyOf(metadata);
    }

    /**
     * Create an embedding response with the given embeddings.
     *
     * @param embeddings the embedding vectors
     * @return a new EmbeddingResponse
     */
    public static EmbeddingResponse of(List<EmbeddingVector> embeddings) {
        return new EmbeddingResponse(embeddings, TokenUsage.empty(), null, null);
    }

    /**
     * Get the first embedding vector.
     *
     * @return the first embedding, or empty if no embeddings
     */
    public Optional<EmbeddingVector> first() {
        return embeddings.isEmpty() ? Optional.empty() : Optional.of(embeddings.get(0));
    }
}
