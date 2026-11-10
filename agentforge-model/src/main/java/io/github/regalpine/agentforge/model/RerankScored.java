package io.github.regalpine.agentforge.model;

import java.util.Objects;

/**
 * A scored document from reranking.
 */
public record RerankScored(
    int index,
    String document,
    double score
) {

    public RerankScored {
        if (index < 0) {
            throw new IllegalArgumentException("index cannot be negative");
        }
        Objects.requireNonNull(document, "document");
    }

    /**
     * Create a scored result.
     *
     * @param index    the original index in the input
     * @param document the document text
     * @param score    the relevance score
     * @return a new RerankScored
     */
    public static RerankScored of(int index, String document, double score) {
        return new RerankScored(index, document, score);
    }
}
