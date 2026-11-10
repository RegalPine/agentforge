package io.github.regalpine.agentforge.rag.model;

import java.util.Map;
import java.util.Objects;

/**
 * Retrieval budget — limits for a single retrieval operation.
 * AE-09 §56: Retrieval Budget.
 */
public record RetrievalBudget(
    int maxTopK,
    int maxDocuments,
    int maxChunks,
    int maxTokens,
    long maxLatencyMs
) {

    public RetrievalBudget {
        if (maxTopK <= 0) maxTopK = 10;
        if (maxDocuments <= 0) maxDocuments = 50;
        if (maxChunks <= 0) maxChunks = 100;
        if (maxTokens <= 0) maxTokens = 8_000;
        if (maxLatencyMs <= 0) maxLatencyMs = 30_000L;
    }

    public static RetrievalBudget defaults() {
        return new RetrievalBudget(10, 50, 100, 8_000, 30_000L);
    }

    public static RetrievalBudget small() {
        return new RetrievalBudget(5, 10, 20, 2_000, 10_000L);
    }

    /**
     * Clamp topK to the budget limit.
     */
    public int effectiveTopK(int requested) {
        return Math.min(requested, maxTopK);
    }
}
