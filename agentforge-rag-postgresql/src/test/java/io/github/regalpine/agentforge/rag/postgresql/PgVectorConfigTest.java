package io.github.regalpine.agentforge.rag.postgresql;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * PGVector config and SQL tests.
 * AF-ENHANCE-03 §10: verifies config
 * validation and SQL generation.
 */
class PgVectorConfigTest {

    @Test
    void shouldCreateConfig() {
        var config = PgVectorConfig.of(
            "jdbc:postgresql://localhost/test",
            "public"
        );

        assertEquals("public", config.schema());
        assertEquals("knowledge_chunks",
            config.tableName());
        assertEquals(1536,
            config.embeddingDimension());
        assertEquals("hnsw",
            config.indexMethod());
    }

    @Test
    void shouldGenerateCreateTableSql() {
        String sql = PgVectorSql
            .createTableSqlDim(
                "public.knowledge_chunks", 768
            );

        assertTrue(sql.contains("CREATE TABLE"));
        assertTrue(sql.contains("vector(768)"));
        assertTrue(sql.contains("chunk_id"));
        assertTrue(sql.contains("embedding"));
    }

    @Test
    void shouldGenerateVectorIndexSql() {
        String sql = PgVectorSql
            .createVectorIndexSql(
                "public.knowledge_chunks",
                "hnsw", 64, 16
            );

        assertTrue(sql.contains("USING hnsw"));
        assertTrue(sql.contains("m = 16"));
        assertTrue(sql.contains(
            "ef_construction = 64"
        ));
    }

    @Test
    void shouldGenerateUpsertSql() {
        String sql = PgVectorSql.upsertSql(
            "public.knowledge_chunks"
        );

        assertTrue(sql.contains("INSERT INTO"));
        assertTrue(sql.contains(
            "ON CONFLICT (chunk_id)"
        ));
        assertTrue(sql.contains("DO UPDATE SET"));
    }

    @Test
    void shouldGenerateHybridSearchSql() {
        String sql = PgVectorSql
            .hybridSearchSql(
                "public.knowledge_chunks"
            );

        assertTrue(sql.contains("WITH vec AS"));
        assertTrue(sql.contains("kw AS"));
        assertTrue(sql.contains("score"));
    }

    @Test
    void shouldFormatVector() {
        float[] vec = {1.0f, 2.0f, 3.0f};

        String result =
            PgVectorKnowledgeIndex
                .formatVector(vec);

        assertEquals("[1.0,2.0,3.0]", result);
    }

    @Test
    void shouldFormatEmptyVector() {
        assertEquals("[]",
            PgVectorKnowledgeIndex
                .formatVector(null));
        assertEquals("[]",
            PgVectorKnowledgeIndex
                .formatVector(new float[]{}));
    }
}
