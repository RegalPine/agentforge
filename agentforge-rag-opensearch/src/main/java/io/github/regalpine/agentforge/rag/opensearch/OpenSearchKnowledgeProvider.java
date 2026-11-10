package io.github.regalpine.agentforge.rag.opensearch;

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
 * OpenSearch knowledge provider.
 * AF-ENHANCE-03 §10: bundles the
 * OpenSearch index with the standard
 * document parser and retrieval
 * pipeline.
 */
public final class OpenSearchKnowledgeProvider
    implements KnowledgeProvider {

    private final String providerId;
    private final DocumentParser parser;
    private final OpenSearchKnowledgeIndex index;
    private final KnowledgeRetriever retriever;
    private final KnowledgeCapabilities capabilities;

    public OpenSearchKnowledgeProvider(
        String providerId,
        OpenSearchConfig config,
        KnowledgePolicy policy
    ) {
        this.providerId =
            Objects.requireNonNull(providerId);
        Objects.requireNonNull(config, "config");

        this.parser = CompositeDocumentParser.defaults();
        this.index = new OpenSearchKnowledgeIndex(config);
        this.retriever = new RetrievalPipeline(
            index, policy, null
        );

        this.capabilities = new KnowledgeCapabilities(
            true, true, true,
            false, true, true,
            Set.of(
                "text/plain",
                "text/markdown",
                "text/html",
                "application/pdf"
            )
        );
    }

    public OpenSearchKnowledgeProvider(
        String providerId,
        OpenSearchConfig config
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
