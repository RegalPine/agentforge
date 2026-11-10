package io.github.regalpine.agentforge.rag.postgresql;

import java.util.Map;
import java.util.Objects;

/**
 * PGVector RAG store configuration.
 * AF-ENHANCE-03 §10: configures the
 * PostgreSQL + pgvector connection
 * for knowledge chunk storage and
 * vector similarity search.
 */
public record PgVectorConfig(
    String jdbcUrl,
    String username,
    String schema,
    String tableName,
    int embeddingDimension,
    String indexMethod,
    int efConstruction,
    int mParam,
    Map<String, Object> properties
) {

    public PgVectorConfig {
        Objects.requireNonNull(jdbcUrl, "jdbcUrl");
        Objects.requireNonNull(schema, "schema");

        if (embeddingDimension <= 0) {
            embeddingDimension = 1536;
        }

        if (tableName == null
            || tableName.isBlank()
        ) {
            tableName = "knowledge_chunks";
        }

        if (indexMethod == null
            || indexMethod.isBlank()
        ) {
            indexMethod = "hnsw";
        }

        if (efConstruction <= 0) {
            efConstruction = 64;
        }

        if (mParam <= 0) {
            mParam = 16;
        }

        properties =
            properties == null
                ? Map.of()
                : Map.copyOf(properties);
    }

    public static PgVectorConfig of(
        String jdbcUrl, String schema
    ) {
        return new PgVectorConfig(
            jdbcUrl, null, schema,
            "knowledge_chunks",
            1536, "hnsw", 64, 16,
            Map.of()
        );
    }

    public String qualifiedTable() {
        return schema + "." + tableName;
    }
}
