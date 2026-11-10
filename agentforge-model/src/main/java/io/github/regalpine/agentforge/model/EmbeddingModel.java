package io.github.regalpine.agentforge.model;

import java.util.List;
import java.util.Objects;

/**
 * Interface for embedding models that can convert text to vector representations.
 * <p>
 * Embedding models are used for:
 * <ul>
 *   <li>Semantic search</li>
 *   <li>Knowledge retrieval (RAG)</li>
 *   <li>Text similarity</li>
 *   <li>Clustering</li>
 * </ul>
 */
public interface EmbeddingModel {

    /**
     * Generate embeddings for the given texts.
     *
     * @param request the embedding request
     * @return the embedding response
     * @throws ModelException if the model invocation fails
     */
    EmbeddingResponse embed(EmbeddingRequest request);

    /**
     * Get the model identifier.
     *
     * @return the model ID
     */
    String modelId();

    /**
     * Get the provider identifier.
     *
     * @return the provider ID
     */
    String providerId();
}
