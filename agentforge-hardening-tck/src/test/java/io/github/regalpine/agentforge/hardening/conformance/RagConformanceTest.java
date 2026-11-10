package io.github.regalpine.agentforge.hardening.conformance;

import io.github.regalpine.agentforge.hardening.*;
import io.github.regalpine.agentforge.rag.model.*;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * conformance-rag: Loader / Chunk
 * / Retriever / Reranker / Citation.
 * AF-RELEASE-02 §4, T-H6.8.
 */
class RagConformanceTest {

    private final ChunkLocation loc =
        ChunkLocation.of(1, "intro");

    @Test
    void afRag001_chunkCreation() {
        var chunk = new KnowledgeChunk(
            "chunk-001", "doc-001",
            "src-001", "t1",
            "Test content",
            loc, "", DocumentClassification.PUBLIC,
            "en", null, null
        );
        assertEquals("chunk-001", chunk.chunkId());
        assertEquals("doc-001", chunk.documentId());
        assertEquals("Test content", chunk.text());
    }

    @Test
    void afRag002_queryCreation() {
        var scope = new KnowledgeScope(
            "t1", null, null, null
        );
        var query = new KnowledgeQuery(
            "search term", scope, null, 10
        );
        assertNotNull(query);
        assertEquals("search term", query.query());
    }

    @Test
    void afRag003_retrievalResult() {
        var chunk = new KnowledgeChunk(
            "chunk-1", "doc-1",
            "src-1", "t1",
            "Result text",
            loc, "", DocumentClassification.PUBLIC,
            "en", null, null
        );
        var scores = RetrievalScoreBreakdown.semantic(0.95);
        var result = RetrievalResult.of(
            chunk, 0.95, scores
        );
        assertEquals("chunk-1", result.chunkId());
        assertEquals(0.95, result.score(), 0.001);
    }

    @Test
    void afRag004_chunkMetadata() {
        var chunk = new KnowledgeChunk(
            "c-meta", "doc-1",
            "src-1", "t1",
            "Content",
            loc, "", DocumentClassification.INTERNAL,
            "en", null, null
        );
        assertEquals("en", chunk.language());
        assertEquals(
            DocumentClassification.INTERNAL,
            chunk.classification()
        );
    }

    @Test
    void afRag005_knowledgeScope() {
        var scope = new KnowledgeScope(
            "tenant-1", null, null, null
        );
        assertEquals("tenant-1", scope.tenantId());
    }
}
