package io.github.regalpine.agentforge.rag.postgresql;

import io.github.regalpine.agentforge.rag.index.KnowledgeIndex;
import io.github.regalpine.agentforge.rag.ingestion.CompositeDocumentParser;
import io.github.regalpine.agentforge.rag.ingestion.DocumentParser;
import io.github.regalpine.agentforge.rag.policy.KnowledgePolicy;
import io.github.regalpine.agentforge.rag.provider.KnowledgeCapabilities;
import io.github.regalpine.agentforge.rag.provider.KnowledgeProvider;
import io.github.regalpine.agentforge.rag.retrieval.KnowledgeRetriever;
import io.github.regalpine.agentforge.rag.retrieval.RetrievalPipeline;

import javax.sql.DataSource;
import java.util.Objects;
import java.util.Set;

/**
 * PGVector knowledge provider.
 * AF-ENHANCE-03 §10: bundles the
 * PGVector index with the standard
 * document parser and retrieval
 * pipeline.
 */
public final class PgVectorKnowledgeProvider
    implements KnowledgeProvider {

    private final String providerId;
    private final DocumentParser parser;
    private final PgVectorKnowledgeIndex index;
    private final KnowledgeRetriever retriever;
    private final KnowledgeCapabilities capabilities;

    public PgVectorKnowledgeProvider(
        String providerId,
        PgVectorConfig config,
        DataSource dataSource,
        KnowledgePolicy policy
    ) {
        this.providerId =
            Objects.requireNonNull(providerId);
        Objects.requireNonNull(config, "config");
        Objects.requireNonNull(dataSource, "dataSource");

        this.parser = CompositeDocumentParser.defaults();
        this.index = new PgVectorKnowledgeIndex(
            config, dataSource
        );
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

    public PgVectorKnowledgeProvider(
        String providerId,
        PgVectorConfig config,
        DataSource dataSource
    ) {
        this(
            providerId, config, dataSource,
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

    /**
     * Initialise the database schema.
     */
    public void initialise() {
        try {
            index.initialiseSchema();
        } catch (Exception e) {
            throw new PgVectorKnowledgeIndex
                .PgVectorException(
                    "Schema init failed", e
                );
        }
    }
}
