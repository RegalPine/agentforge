package io.github.regalpine.agentforge.memory;

/**
 * Semantic memory retriever SPI.
 * AE-08 §29: Semantic Retrieval.
 *
 * <p>AgentForge does not implement a
 * vector database. This SPI allows
 * external providers (PostgreSQL + pgvector,
 * dedicated vector DB, search engine) to
 * plug in semantic retrieval.
 */
public interface SemanticMemoryRetriever {

    /**
     * Retrieve memories by semantic
     * similarity to the query text.
     *
     * @param query the query text
     * @param subjectId scope subject
     * @param scope memory scope
     * @param limit maximum results
     * @return ranked list of records
     */
    java.util.List<MemoryRecord> retrieveBySimilarity(
        String query,
        String subjectId,
        MemoryScope scope,
        int limit
    );
}
