package io.github.regalpine.agentforge.rag.opensearch;

import org.junit.jupiter.api.Test;

import java.net.URI;

import static org.junit.jupiter.api.Assertions.*;

/**
 * OpenSearch config and JSON tests.
 * AF-ENHANCE-03 §10: verifies config
 * and JSON payload generation.
 */
class OpenSearchConfigTest {

    @Test
    void shouldCreateConfig() {
        var config = OpenSearchConfig.of(
            URI.create("http://localhost:9200"),
            "knowledge"
        );

        assertEquals("knowledge",
            config.indexName());
        assertEquals(1536,
            config.embeddingDimension());
        assertEquals("nmslib",
            config.knnEngine());
    }

    @Test
    void shouldGenerateIndexMapping() {
        String json = OpenSearchJson
            .createIndexMapping(1536);

        assertTrue(json.contains("knn_vector"));
        assertTrue(json.contains("\"dimension\":1536"));
        assertTrue(json.contains("cosinesimil"));
    }

    @Test
    void shouldGenerateKeywordSearchJson() {
        String json = OpenSearchJson
            .keywordSearchJson("test query", 5);

        assertTrue(json.contains("\"match\""));
        assertTrue(json.contains("test query"));
        assertTrue(json.contains("\"size\":5"));
    }

    @Test
    void shouldGenerateHybridSearchJson() {
        float[] vec = {0.1f, 0.2f};

        String json = OpenSearchJson
            .hybridSearchJson(
                "test", vec, 10
            );

        assertTrue(json.contains("bool"));
        assertTrue(json.contains("should"));
        assertTrue(json.contains("knn"));
        assertTrue(json.contains("match"));
    }

    @Test
    void shouldEscapeSpecialChars() {
        String json = OpenSearchJson
            .keywordSearchJson(
                "test \"quoted\"", 5
            );

        assertTrue(json.contains(
            "test \\\"quoted\\\""
        ));
    }
}
