package io.github.regalpine.agentforge.memory.postgresql;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * PostgreSQL memory config and SQL tests.
 * AF-ENHANCE-03 §5-7: verifies config
 * validation, DDL generation and SQL
 * statement construction.
 */
class PostgresMemoryConfigTest {

    @Test
    void shouldCreateConfig() {
        var config = PostgresMemoryConfig.of(
            "jdbc:postgresql://localhost/test",
            "public"
        );

        assertEquals("public", config.schema());
        assertEquals("memory_records",
            config.tableName());
        assertEquals("public.memory_records",
            config.qualifiedTable());
        assertFalse(config.enablePgVector());
    }

    @Test
    void shouldEnablePgVector() {
        var config = PostgresMemoryConfig
            .of("jdbc:postgresql://localhost/test",
                "public")
            .withPgVector(768);

        assertTrue(config.enablePgVector());
        assertEquals(768,
            config.embeddingDimension());
    }

    @Test
    void shouldRejectBlankJdbcUrl() {
        assertThrows(IllegalArgumentException.class,
            () -> PostgresMemoryConfig.of(
                "  ", "public"
            )
        );
    }

    @Test
    void shouldGenerateCreateTableSql() {
        String sql = PostgresMemorySql
            .createTableSql("public.memory_records");

        assertTrue(sql.contains("CREATE TABLE"));
        assertTrue(sql.contains("memory_id"));
        assertTrue(sql.contains("tenant_id"));
        assertTrue(sql.contains("content"));
        assertTrue(sql.contains("JSONB"));
    }

    @Test
    void shouldGenerateInsertSql() {
        String sql = PostgresMemorySql.insertSql(
            "public.memory_records"
        );

        assertTrue(sql.contains("INSERT INTO"));
        assertTrue(sql.contains("VALUES"));
    }

    @Test
    void shouldGenerateKeywordSearchSql() {
        String sql = PostgresMemorySql
            .keywordSearchSql("public.memory_records");

        assertTrue(sql.contains("to_tsvector"));
        assertTrue(sql.contains("plainto_tsquery"));
        assertTrue(sql.contains("ts_rank"));
    }

    @Test
    void shouldGenerateHybridSearchSql() {
        String sql = PostgresMemorySql
            .hybridSearchSql("public.memory_records");

        assertTrue(sql.contains("WITH kw AS"));
        assertTrue(sql.contains("vec AS"));
        assertTrue(sql.contains("combined_score"));
    }

    @Test
    void shouldGenerateVectorColumnSql() {
        String sql = PostgresMemorySql
            .createVectorColumnSql(
                "public.memory_records", 1536
            );

        assertTrue(sql.contains("ALTER TABLE"));
        assertTrue(sql.contains("vector(1536)"));
    }
}
