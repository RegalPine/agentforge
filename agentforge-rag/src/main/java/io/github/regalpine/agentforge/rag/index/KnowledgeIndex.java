package io.github.regalpine.agentforge.rag.index;

import io.github.regalpine.agentforge.rag.model.KnowledgeChunk;
import io.github.regalpine.agentforge.rag.model.KnowledgeQuery;
import io.github.regalpine.agentforge.rag.model.RetrievalResult;

import java.util.List;

/**
 * Knowledge index SPI.
 * AE-09 §26: Index.
 *
 * <p>Provides upsert, delete, and search operations on knowledge chunks.
 * Implementations may be keyword-based, vector-based, or hybrid.
 */
public interface KnowledgeIndex {

    /**
     * Insert or update a chunk in the index.
     */
    void upsert(KnowledgeChunk chunk);

    /**
     * Delete a chunk from the index by its ID.
     */
    void delete(String chunkId);

    /**
     * Search the index with the given query.
     *
     * @param query the retrieval query
     * @return list of retrieval results, ordered by relevance
     */
    List<RetrievalResult> search(KnowledgeQuery query);

    /**
     * Get the index type identifier.
     */
    String indexType();

    /**
     * Get the total number of indexed chunks.
     */
    int size();
}
