package io.github.regalpine.agentforge.rag;

import io.github.regalpine.agentforge.rag.citation.CitationValidator;
import io.github.regalpine.agentforge.rag.event.CollectingKnowledgeEventSink;
import io.github.regalpine.agentforge.rag.event.KnowledgeEvent;
import io.github.regalpine.agentforge.rag.event.KnowledgeEventType;
import io.github.regalpine.agentforge.rag.index.*;
import io.github.regalpine.agentforge.rag.ingestion.*;
import io.github.regalpine.agentforge.rag.model.*;
import io.github.regalpine.agentforge.rag.policy.KnowledgePolicy;
import io.github.regalpine.agentforge.rag.provider.InMemoryKnowledgeProvider;
import io.github.regalpine.agentforge.rag.retrieval.RetrievalPipeline;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * RagModelTest — tests core RAG models.
 */
class RagModelTest {

    @Test
    void knowledgeSourceShouldStoreFields() {
        var source = new KnowledgeSource(
                "src-1", "tenant-1", "Policy Docs",
                KnowledgeSourceType.FILE, "1.0",
                KnowledgeSourceStatus.ACTIVE, KnowledgeAuthority.AUTHORITATIVE,
                Map.of("owner", "legal"));

        assertEquals("src-1", source.sourceId());
        assertEquals("tenant-1", source.tenantId());
        assertEquals("Policy Docs", source.name());
        assertEquals(KnowledgeSourceType.FILE, source.type());
        assertEquals(KnowledgeSourceStatus.ACTIVE, source.status());
        assertEquals(KnowledgeAuthority.AUTHORITATIVE, source.authority());
        assertEquals("legal", source.metadataValue("owner").orElse(null));
    }

    @Test
    void knowledgeDocumentShouldTrackValidity() {
        var doc = new KnowledgeDocument(
                "doc-1", "src-1", "tenant-1", "ext-1",
                "Test Doc", "text/plain", "1.0", "abc123",
                DocumentStatus.ACTIVE, DocumentClassification.INTERNAL,
                KnowledgeAuthority.TRUSTED,
                java.time.Instant.parse("2025-01-01T00:00:00Z"),
                java.time.Instant.parse("2025-12-31T23:59:59Z"),
                null, null, null, Map.of());

        assertTrue(doc.isValidAt(java.time.Instant.parse("2025-06-15T00:00:00Z")));
        assertFalse(doc.isValidAt(java.time.Instant.parse("2026-01-01T00:00:00Z")));
        assertFalse(doc.isValidAt(java.time.Instant.parse("2024-12-31T00:00:00Z")));
    }

    @Test
    void knowledgeChunkShouldEstimateTokens() {
        var chunk = new KnowledgeChunk(
                "chunk-1", "doc-1", "src-1", "tenant-1",
                "This is a test text with some content for token estimation.",
                ChunkLocation.of(1, "Section 1"), "checksum",
                DocumentClassification.PUBLIC, "en", null, Map.of());

        assertTrue(chunk.estimatedTokens() > 0);
        assertEquals("chunk-1", chunk.chunkId());
        assertFalse(chunk.hasEmbedding());
    }

    @Test
    void knowledgeScopeShouldEnforceTenantId() {
        var scope = KnowledgeScope.tenant("tenant-1");
        assertEquals("tenant-1", scope.tenantId());
        assertFalse(scope.restrictsSources());
        assertFalse(scope.restrictsDocuments());
        assertFalse(scope.restrictsClassifications());
    }

    @Test
    void retrievalBudgetShouldClampTopK() {
        var budget = RetrievalBudget.small();
        assertEquals(5, budget.effectiveTopK(100));
        assertEquals(3, budget.effectiveTopK(3));
    }

    @Test
    void scoreBreakdownShouldComputeCombined() {
        var breakdown = RetrievalScoreBreakdown.of(0.8, 0.6);
        assertEquals(0.7, breakdown.combined(), 0.001);
    }
}
