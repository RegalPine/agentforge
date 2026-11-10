package io.github.regalpine.agentforge.rag.retrieval;

import io.github.regalpine.agentforge.rag.model.KnowledgeQuery;
import io.github.regalpine.agentforge.rag.model.RetrievalResult;

import java.util.List;

/**
 * Knowledge retriever SPI.
 * AE-09 §70: KnowledgeProvider.retriever().
 */
public interface KnowledgeRetriever {

    /**
     * Retrieve knowledge chunks matching the query.
     *
     * @param query the retrieval query
     * @return ranked list of retrieval results
     */
    List<RetrievalResult> retrieve(KnowledgeQuery query);
}
