package io.github.regalpine.agentforge.model;

import java.util.List;

/**
 * Interface for reranking models that can reorder documents by relevance.
 * <p>
 * Reranking models are used for:
 * <ul>
 *   <li>Improving retrieval quality in RAG</li>
 *   <li>Reordering search results</li>
 *   <li>Document relevance scoring</li>
 * </ul>
 */
public interface RerankModel {

    /**
     * Rerank documents by relevance to the query.
     *
     * @param request the rerank request
     * @return the rerank response
     * @throws ModelException if the model invocation fails
     */
    RerankResponse rerank(RerankRequest request);

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
