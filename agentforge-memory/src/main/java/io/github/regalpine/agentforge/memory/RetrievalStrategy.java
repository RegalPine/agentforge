package io.github.regalpine.agentforge.memory;

/**
 * Retrieval strategy.
 * AE-08 §26: Retrieval Strategy.
 *
 * <p>MVP supports EXACT and KEYWORD.
 * SEMANTIC and HYBRID are defined for
 * future integration with embedding models.
 */
public enum RetrievalStrategy {

    /**
     * Match by exact attributes
     * (memoryId, subjectId, type, etc.).
     */
    EXACT,

    /**
     * Keyword-based text matching.
     */
    KEYWORD,

    /**
     * Semantic similarity via embedding.
     * Requires a SemanticMemoryRetriever.
     */
    SEMANTIC,

    /**
     * Combination of metadata filter,
     * keyword and semantic similarity.
     */
    HYBRID
}
