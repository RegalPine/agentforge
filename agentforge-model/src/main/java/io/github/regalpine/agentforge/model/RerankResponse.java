package io.github.regalpine.agentforge.model;

import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Response from a reranking model.
 */
public record RerankResponse(
    List<RerankScored> results,
    TokenUsage usage,
    String model,
    Map<String, Object> metadata
) {

    public RerankResponse {
        Objects.requireNonNull(results, "results");
        Objects.requireNonNull(usage, "usage");

        results = List.copyOf(results);
        metadata = metadata == null ? Map.of() : Map.copyOf(metadata);
    }

    /**
     * Create a rerank response.
     *
     * @param results the scored results
     * @return a new RerankResponse
     */
    public static RerankResponse of(List<RerankScored> results) {
        return new RerankResponse(results, TokenUsage.empty(), null, null);
    }
}
