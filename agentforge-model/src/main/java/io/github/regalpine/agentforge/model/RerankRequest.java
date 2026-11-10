package io.github.regalpine.agentforge.model;

import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Request to a reranking model.
 */
public record RerankRequest(
    String query,
    List<String> documents,
    Integer topK,
    String model,
    Map<String, Object> metadata
) {

    public RerankRequest {
        Objects.requireNonNull(query, "query");
        Objects.requireNonNull(documents, "documents");

        if (query.isBlank()) {
            throw new IllegalArgumentException("query cannot be blank");
        }
        if (documents.isEmpty()) {
            throw new IllegalArgumentException("documents cannot be empty");
        }

        documents = List.copyOf(documents);
        metadata = metadata == null ? Map.of() : Map.copyOf(metadata);
    }

    /**
     * Create a rerank request.
     *
     * @param query     the query to rank against
     * @param documents the documents to rerank
     * @return a new RerankRequest
     */
    public static RerankRequest of(String query, List<String> documents) {
        return new RerankRequest(query, documents, null, null, null);
    }

    /**
     * Create a rerank request with topK.
     *
     * @param query     the query to rank against
     * @param documents the documents to rerank
     * @param topK      maximum number of results to return
     * @return a new RerankRequest
     */
    public static RerankRequest of(String query, List<String> documents, int topK) {
        return new RerankRequest(query, documents, topK, null, null);
    }
}
