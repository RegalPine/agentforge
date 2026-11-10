package io.github.regalpine.agentforge.rag.provider;

import io.github.regalpine.agentforge.rag.index.InMemoryKeywordIndex;
import io.github.regalpine.agentforge.rag.index.KnowledgeIndex;
import io.github.regalpine.agentforge.rag.ingestion.CompositeDocumentParser;
import io.github.regalpine.agentforge.rag.ingestion.DocumentParser;
import io.github.regalpine.agentforge.rag.policy.KnowledgePolicy;
import io.github.regalpine.agentforge.rag.retrieval.KnowledgeRetriever;
import io.github.regalpine.agentforge.rag.retrieval.RetrievalPipeline;

/**
 * In-memory knowledge provider for testing and development.
 * AE-09 §70: RAG Provider.
 */
public final class InMemoryKnowledgeProvider implements KnowledgeProvider {

    private final String providerId;
    private final DocumentParser parser;
    private final InMemoryKeywordIndex index;
    private final RetrievalPipeline retriever;
    private final KnowledgeCapabilities capabilities;

    public InMemoryKnowledgeProvider(String providerId, KnowledgePolicy policy) {
        this.providerId = providerId;
        this.parser = CompositeDocumentParser.defaults();
        this.index = new InMemoryKeywordIndex();
        this.retriever = new RetrievalPipeline(index, policy, null);
        this.capabilities = KnowledgeCapabilities.basic();
    }

    public InMemoryKnowledgeProvider(String providerId) {
        this(providerId, KnowledgePolicy.permissive());
    }

    public static InMemoryKnowledgeProvider create() {
        return new InMemoryKnowledgeProvider("in-memory");
    }

    @Override
    public DocumentParser parser() {
        return parser;
    }

    @Override
    public KnowledgeIndex index() {
        return index;
    }

    @Override
    public KnowledgeRetriever retriever() {
        return retriever;
    }

    @Override
    public KnowledgeCapabilities capabilities() {
        return capabilities;
    }

    @Override
    public String providerId() {
        return providerId;
    }
}
