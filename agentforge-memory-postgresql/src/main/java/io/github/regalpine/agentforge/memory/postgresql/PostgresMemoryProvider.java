package io.github.regalpine.agentforge.memory.postgresql;

import io.github.regalpine.agentforge.memory.*;

import javax.sql.DataSource;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * PostgreSQL memory provider.
 * AF-ENHANCE-03 §5: bundles the store,
 * retriever and capabilities into a
 * single provider.
 */
public final class PostgresMemoryProvider
    implements MemoryProvider {

    private final PostgresMemoryStore store;
    private final PostgresMemoryRetriever retriever;
    private final MemoryProvider.MemoryCapabilities capabilities;

    public PostgresMemoryProvider(
        PostgresMemoryConfig config,
        DataSource dataSource
    ) {
        Objects.requireNonNull(config, "config");
        Objects.requireNonNull(dataSource, "dataSource");

        this.store = new PostgresMemoryStore(
            config, dataSource
        );
        this.retriever = new PostgresMemoryRetriever(
            config, dataSource
        );

        boolean supportsSemantic =
            config.enablePgVector();

        this.capabilities =
            new MemoryProvider.MemoryCapabilities(
                supportsSemantic,
                true,
                true,
                Set.of(MemoryType.values()),
                java.util.Map.of(
                    "engine", "postgresql",
                    "pgvector", supportsSemantic,
                    "schema", config.schema()
                )
            );
    }

    @Override
    public String providerType() {
        return "postgresql";
    }

    @Override
    public MemoryStore store() {
        return store;
    }

    @Override
    public MemoryRetriever retriever() {
        return retriever;
    }

    @Override
    public MemoryCapabilities capabilities() {
        return capabilities;
    }

    /**
     * Initialise the database schema.
     */
    public void initialise() {
        try {
            store.initialiseSchema();
        } catch (Exception e) {
            throw new PostgresMemoryStore
                .PostgresMemoryException(
                    "Schema initialisation failed", e
                );
        }
    }
}
