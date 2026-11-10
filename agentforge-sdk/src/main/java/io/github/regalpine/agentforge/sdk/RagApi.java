package io.github.regalpine.agentforge.sdk;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * RAG / Knowledge API facade.
 * AF-ENHANCE-04 §4: provides a convenient
 * entry point for knowledge operations.
 *
 * <p>This facade does NOT hold semantic
 * definitions — it delegates to the
 * existing RAG SPI.
 */
public final class RagApi {

    private final Map<String, Object> chunks =
        new ConcurrentHashMap<>();
    private final String indexType;

    /**
     * Create with default index type.
     */
    public RagApi() {
        this("default");
    }

    /**
     * Create with a specific index type.
     */
    public RagApi(String indexType) {
        this.indexType =
            Objects.requireNonNull(indexType);
    }

    /**
     * Upsert a knowledge chunk.
     */
    public RagApi upsert(
        String chunkId, Object content
    ) {
        chunks.put(chunkId, content);
        return this;
    }

    /**
     * Delete a knowledge chunk.
     */
    public RagApi delete(String chunkId) {
        chunks.remove(chunkId);
        return this;
    }

    /**
     * Search for chunks matching a query.
     */
    public List<Object> search(String query) {
        // In-memory search stub.
        // Real implementation delegates to
        // KnowledgeIndex SPI.
        return List.copyOf(chunks.values());
    }

    /**
     * Get the number of indexed chunks.
     */
    public int size() {
        return chunks.size();
    }

    /**
     * Get the index type.
     */
    public String indexType() {
        return indexType;
    }

    /**
     * Clear all chunks.
     */
    public RagApi clear() {
        chunks.clear();
        return this;
    }
}
