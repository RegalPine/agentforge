package io.github.regalpine.agentforge.rag.provider;

import io.github.regalpine.agentforge.rag.index.KnowledgeIndex;
import io.github.regalpine.agentforge.rag.ingestion.DocumentParser;
import io.github.regalpine.agentforge.rag.retrieval.KnowledgeRetriever;

/**
 * Knowledge provider SPI.
 * AE-09 §70: RAG Provider.
 *
 * <p>Aggregates the parser, index, and retriever into a single
 * provider that can be plugged into the Agent Runtime.
 */
public interface KnowledgeProvider {

    /**
     * The document parser for this provider.
     */
    DocumentParser parser();

    /**
     * The knowledge index for this provider.
     */
    KnowledgeIndex index();

    /**
     * The knowledge retriever for this provider.
     */
    KnowledgeRetriever retriever();

    /**
     * Provider capabilities description.
     */
    KnowledgeCapabilities capabilities();

    /**
     * Provider identifier.
     */
    String providerId();
}
