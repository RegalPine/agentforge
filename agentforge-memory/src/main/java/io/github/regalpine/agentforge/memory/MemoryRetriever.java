package io.github.regalpine.agentforge.memory;

import java.util.List;

/**
 * Memory retriever.
 * AE-08 §25: Memory Retrieval.
 *
 * <p>Unified retrieval interface.
 * Implementations apply scope filtering,
 * policy checks and ranking.
 */
public interface MemoryRetriever {

    /**
     * Retrieve memories matching the query.
     *
     * <p>Flow:
     * Query → Scope Filter → Policy →
     * Candidate Retrieval → Ranking →
     * Budget Filter → Result
     */
    List<MemoryRecord> retrieve(MemoryQuery query);
}
