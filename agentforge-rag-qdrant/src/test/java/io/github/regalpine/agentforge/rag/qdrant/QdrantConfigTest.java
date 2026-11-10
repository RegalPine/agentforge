package io.github.regalpine.agentforge.rag.qdrant;

import org.junit.jupiter.api.Test;

import java.net.URI;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Qdrant config and JSON tests.
 * AF-ENHANCE-03 §10: verifies config
 * and JSON payload generation.
 */
class QdrantConfigTest {

    @Test
    void shouldCreateConfig() {
        var config = QdrantConfig.of(
            URI.create("http://localhost:6333"),
            "knowledge"
        );

        assertEquals("knowledge",
            config.collectionName());
        assertEquals(1536,
            config.embeddingDimension());
        assertEquals("Cosine",
            config.distance());
    }

    @Test
    void shouldGenerateCreateCollectionJson() {
        String json = QdrantJson
            .createCollectionJson(768);

        assertTrue(json.contains("\"size\":768"));
        assertTrue(json.contains("Cosine"));
        assertTrue(json.contains("vectors"));
    }

    @Test
    void shouldGenerateUpsertPointJson() {
        float[] vec = {0.1f, 0.2f, 0.3f};

        String json = QdrantJson.upsertPointJson(
            "c1", "d1", "s1", "t1",
            "test text", 0, "section",
            0, "abc", "PUBLIC", vec
        );

        assertTrue(json.contains("\"id\":\"c1\""));
        assertTrue(json.contains(
            "\"tenant_id\":\"t1\""
        ));
        assertTrue(json.contains("0.1"));
        assertTrue(json.contains("0.2"));
        assertTrue(json.contains("0.3"));
    }

    @Test
    void shouldGenerateSearchJson() {
        float[] vec = {0.5f, 0.5f};

        String json = QdrantJson.searchJson(
            vec, 10, "tenant-1"
        );

        assertTrue(json.contains("\"limit\":10"));
        assertTrue(json.contains("tenant-1"));
        assertTrue(json.contains("filter"));
    }

    @Test
    void shouldGenerateSearchJsonWithoutFilter() {
        float[] vec = {0.5f};

        String json = QdrantJson.searchJson(
            vec, 5, null
        );

        assertTrue(json.contains("\"limit\":5"));
        assertFalse(json.contains("filter"));
    }
}
