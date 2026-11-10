package io.github.regalpine.agentforge.rag.qdrant;

import io.github.regalpine.agentforge.rag.index.KnowledgeIndex;
import io.github.regalpine.agentforge.rag.ingestion.CompositeDocumentParser;
import io.github.regalpine.agentforge.rag.ingestion.DocumentParser;
import io.github.regalpine.agentforge.rag.policy.KnowledgePolicy;
import io.github.regalpine.agentforge.rag.provider.KnowledgeCapabilities;
import io.github.regalpine.agentforge.rag.provider.KnowledgeProvider;
import io.github.regalpine.agentforge.rag.retrieval.KnowledgeRetriever;
import io.github.regalpine.agentforge.rag.retrieval.RetrievalPipeline;

import java.util.Objects;
import java.util.Set;

/**
 * Qdrant knowledge provider.
 * AF-ENHANCE-03 §10: bundles the
 * Qdrant index with the standard
 * document parser and retrieval
 * pipeline.
 */
public final class QdrantKnowledgeProvider
    implements KnowledgeProvider {

    private final String providerId;
    private final DocumentParser parser;
    private final QdrantKnowledgeIndex index;
    private final KnowledgeRetriever retriever;
    private final KnowledgeCapabilities capabilities;

    public QdrantKnowledgeProvider(
        String providerId,
        QdrantConfig config,
        KnowledgePolicy policy
    ) {
        this.providerId =
            Objects.requireNonNull(providerId);
        Objects.requireNonNull(config, "config");

        this.parser = CompositeDocumentParser.defaults();
        this.index = new QdrantKnowledgeIndex(config);
        this.retriever = new RetrievalPipeline(
            index, policy, null
        );

        this.capabilities = new KnowledgeCapabilities(
            false, true, false,
            false, true, true,
            Set.of(
                "text/plain",
                "text/markdown",
                "text/html",
                "application/pdf"
            )
        );
    }

    public QdrantKnowledgeProvider(
        String providerId,
        QdrantConfig config
    ) {
        this(
            providerId, config,
            KnowledgePolicy.permissive()
        );
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
