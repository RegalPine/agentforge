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
 * RagIntegrationTest — tests parser, chunker, index, retrieval, citation, events, provider.
 */
class RagIntegrationTest {

    // --- Parser tests ---

    @Test
    void markdownParserShouldExtractHeadings() {
        var parser = new MarkdownParser();
        var parsed = parser.parse("doc-1", "text/markdown",
                "# Title\n\nSome paragraph.\n\n## Section 1\n\nMore text.\n");

        assertTrue(parsed.hasStructure());
        assertTrue(parsed.elementCount() >= 3);
        assertEquals("Title", parsed.title());
    }

    @Test
    void textParserShouldReturnSingleParagraph() {
        var parser = new TextParser();
        var parsed = parser.parse("doc-1", "text/plain", "Hello world content.");

        assertEquals(1, parsed.elementCount());
        assertEquals(DocumentElementType.PARAGRAPH, parsed.elements().get(0).type());
    }

    @Test
    void htmlParserShouldExtractStructure() {
        var parser = new HtmlParser();
        var parsed = parser.parse("doc-1", "text/html",
                "<html><head><title>Test</title></head><body><h1>Title</h1><p>Paragraph content here.</p></body></html>");

        assertEquals("Test", parsed.title());
        assertTrue(parsed.elementCount() > 0);
    }

    @Test
    void compositeParserShouldDelegate() {
        var parser = CompositeDocumentParser.defaults();
        var md = parser.parse("doc-1", "text/markdown", "# Hello\n\nWorld");
        assertEquals("Hello", md.title());

        var txt = parser.parse("doc-2", "text/plain", "Just text");
        assertTrue(txt.text().contains("Just text"));
    }

    // --- Chunker tests ---

    @Test
    void structuralChunkerShouldChunkBySection() {
        var parser = new MarkdownParser();
        var parsed = parser.parse("doc-1", "text/markdown",
                "# Section A\n\nContent of section A.\n\n# Section B\n\nContent of section B.\n");

        var chunker = new StructuralChunker();
        var chunks = chunker.chunk(parsed, "src-1", "tenant-1", DocumentClassification.PUBLIC);

        assertFalse(chunks.isEmpty());
        assertTrue(chunks.size() >= 2);
        assertEquals("src-1", chunks.get(0).sourceId());
        assertEquals("tenant-1", chunks.get(0).tenantId());
    }

    @Test
    void structuralChunkerShouldSplitLargeSections() {
        StringBuilder largeSection = new StringBuilder("# Big Section\n\n");
        for (int i = 0; i < 500; i++) {
            largeSection.append("This is line ").append(i).append(" with enough text to exceed chunk limits.\n");
        }

        var parsed = ParsedDocument.plain("doc-1", largeSection.toString());
        var chunker = new StructuralChunker(100, 10);
        var chunks = chunker.chunk(parsed, "src-1", "tenant-1", DocumentClassification.PUBLIC);

        assertTrue(chunks.size() > 1, "Large section should be split into multiple chunks");
    }

    // --- Index tests ---

    @Test
    void keywordIndexShouldFindMatchingChunks() {
        var index = new InMemoryKeywordIndex();

        var chunk1 = createChunk("chunk-1", "doc-1", "The procurement policy requires approval for amounts over 10000.");
        var chunk2 = createChunk("chunk-2", "doc-2", "The travel policy covers domestic flights.");
        var chunk3 = createChunk("chunk-3", "doc-3", "Procurement guidelines for IT equipment.");

        index.upsert(chunk1);
        index.upsert(chunk2);
        index.upsert(chunk3);

        var results = index.search(KnowledgeQuery.of("procurement policy", "tenant-1"));

        assertFalse(results.isEmpty());
        assertTrue(results.stream().anyMatch(r -> r.chunkId().equals("chunk-1")));
        assertEquals(3, index.size());
    }

    @Test
    void keywordIndexShouldEnforceTenantIsolation() {
        var index = new InMemoryKeywordIndex();

        index.upsert(createChunk("chunk-1", "doc-1", "Tenant A secret data."));
        index.upsert(createChunkWithTenant("chunk-2", "doc-2", "Tenant B secret data.", "tenant-2"));

        var results = index.search(KnowledgeQuery.of("secret data", "tenant-1"));

        assertEquals(1, results.size());
        assertEquals("chunk-1", results.get(0).chunkId());
    }

    @Test
    void keywordIndexShouldDeleteChunks() {
        var index = new InMemoryKeywordIndex();
        index.upsert(createChunk("chunk-1", "doc-1", "Some content here."));
        assertEquals(1, index.size());

        index.delete("chunk-1");
        assertEquals(0, index.size());
    }

    @Test
    void vectorIndexShouldComputeCosineSimilarity() {
        float[] a = {1.0f, 0.0f, 0.0f};
        float[] b = {1.0f, 0.0f, 0.0f};
        float[] c = {0.0f, 1.0f, 0.0f};

        assertEquals(1.0, InMemoryVectorIndex.cosineSimilarity(a, b), 0.001);
        assertEquals(0.0, InMemoryVectorIndex.cosineSimilarity(a, c), 0.001);
    }

    @Test
    void hybridIndexShouldMergeResults() {
        var hybrid = new HybridIndex();
        hybrid.upsert(createChunk("chunk-1", "doc-1", "procurement policy approval"));
        hybrid.upsert(createChunk("chunk-2", "doc-2", "travel reimbursement policy"));

        var results = hybrid.search(KnowledgeQuery.of("procurement policy", "tenant-1"));
        assertFalse(results.isEmpty());
        assertEquals("HYBRID", hybrid.indexType());
    }

    // --- Retrieval pipeline tests ---

    @Test
    void retrievalPipelineShouldDeduplicate() {
        var index = new InMemoryKeywordIndex();
        index.upsert(createChunk("chunk-1", "doc-1", "procurement policy details"));

        var pipeline = new RetrievalPipeline(index);
        var results = pipeline.retrieve(KnowledgeQuery.of("procurement", "tenant-1"));

        assertFalse(results.isEmpty());
        assertEquals(1, results.stream().filter(r -> r.chunkId().equals("chunk-1")).count());
    }

    @Test
    void retrievalPipelineShouldRespectBudget() {
        var index = new InMemoryKeywordIndex();
        for (int i = 0; i < 20; i++) {
            index.upsert(createChunk("chunk-" + i, "doc-1", "procurement policy item " + i));
        }

        var budget = new RetrievalBudget(5, 50, 100, 8000, 30000);
        var pipeline = new RetrievalPipeline(index, null, budget);
        var results = pipeline.retrieve(KnowledgeQuery.of("procurement", "tenant-1"));

        assertTrue(results.size() <= 5);
    }

    // --- Citation tests ---

    @Test
    void citationValidatorShouldValidateGrounding() {
        var validator = new CitationValidator();

        var chunk = createChunk("chunk-1", "doc-1", "Some knowledge text.");
        var result = RetrievalResult.of(chunk, 0.9, RetrievalScoreBreakdown.keyword(0.9));
        var citation = KnowledgeCitation.from(chunk, "Test Doc");

        assertTrue(validator.validate(citation, List.of(result)));
    }

    @Test
    void citationValidatorShouldDetectInvalidCitation() {
        var validator = new CitationValidator();

        var chunk = createChunk("chunk-1", "doc-1", "Some knowledge text.");
        var result = RetrievalResult.of(chunk, 0.9, RetrievalScoreBreakdown.keyword(0.9));

        var fakeChunk = createChunk("chunk-999", "doc-999", "Fake.");
        var fakeCitation = KnowledgeCitation.from(fakeChunk, "Fake Doc");

        assertFalse(validator.validate(fakeCitation, List.of(result)));
    }

    @Test
    void groundingResultShouldTrackRatio() {
        var validator = new CitationValidator();

        var chunk = createChunk("chunk-1", "doc-1", "Knowledge.");
        var result = RetrievalResult.of(chunk, 0.9, RetrievalScoreBreakdown.keyword(0.9));
        var validCitation = KnowledgeCitation.from(chunk, "Doc");
        var fakeChunk = createChunk("chunk-999", "doc-999", "Fake.");
        var invalidCitation = KnowledgeCitation.from(fakeChunk, "Fake");

        var grounding = validator.checkGrounding(List.of(validCitation, invalidCitation), List.of(result));
        assertFalse(grounding.isFullyGrounded());
        assertEquals(1, grounding.validCount());
        assertEquals(1, grounding.invalidCount());
        assertEquals(0.5, grounding.groundingRatio(), 0.001);
    }

    // --- Policy tests ---

    @Test
    void knowledgePolicyShouldControlClassifications() {
        var publicOnly = KnowledgePolicy.publicOnly();
        assertTrue(publicOnly.isClassificationAllowed(DocumentClassification.PUBLIC, null));
        assertFalse(publicOnly.isClassificationAllowed(DocumentClassification.CONFIDENTIAL, null));

        var permissive = KnowledgePolicy.permissive();
        assertTrue(permissive.isClassificationAllowed(DocumentClassification.RESTRICTED, null));
    }

    @Test
    void knowledgePolicyShouldControlAgentPermissions() {
        var policy = new KnowledgePolicy(
                Set.of(DocumentClassification.PUBLIC, DocumentClassification.INTERNAL),
                Set.of(),
                Map.of("agent-1", Set.of(DocumentClassification.PUBLIC))
        );

        assertTrue(policy.isClassificationAllowed(DocumentClassification.PUBLIC, "agent-1"));
        assertFalse(policy.isClassificationAllowed(DocumentClassification.INTERNAL, "agent-1"));
        assertTrue(policy.isClassificationAllowed(DocumentClassification.INTERNAL, "agent-2"));
    }

    // --- Event tests ---

    @Test
    void collectingEventSinkShouldCaptureEvents() {
        var sink = new CollectingKnowledgeEventSink();
        sink.emit(KnowledgeEvent.of(KnowledgeEventType.DOCUMENT_PARSED));
        sink.emit(KnowledgeEvent.of(KnowledgeEventType.DOCUMENT_INDEXED));
        sink.emit(KnowledgeEvent.of(KnowledgeEventType.KNOWLEDGE_RETRIEVAL_STARTED));

        assertEquals(3, sink.count());
        assertEquals(1, sink.eventsOfType(KnowledgeEventType.DOCUMENT_PARSED).size());
        assertEquals(1, sink.eventsOfType(KnowledgeEventType.KNOWLEDGE_RETRIEVAL_STARTED).size());
    }

    // --- Provider tests ---

    @Test
    void inMemoryProviderShouldWork() {
        var provider = InMemoryKnowledgeProvider.create();
        assertEquals("in-memory", provider.providerId());
        assertNotNull(provider.parser());
        assertNotNull(provider.index());
        assertNotNull(provider.retriever());
        assertTrue(provider.capabilities().supportsKeywordSearch());
    }

    @Test
    void providerEndToEndShouldIngestAndRetrieve() {
        var provider = InMemoryKnowledgeProvider.create();

        var parsed = provider.parser().parse("doc-1", "text/plain",
                "The procurement policy requires manager approval for purchases over 5000 dollars.");
        assertNotNull(parsed);

        var chunker = new StructuralChunker();
        var chunks = chunker.chunk(parsed, "src-1", "tenant-1", DocumentClassification.PUBLIC);

        for (var chunk : chunks) {
            provider.index().upsert(chunk);
        }

        var results = provider.retriever().retrieve(
                KnowledgeQuery.of("procurement approval", "tenant-1"));

        assertFalse(results.isEmpty());
        assertTrue(results.get(0).text().contains("procurement"));
    }

    // --- Helpers ---

    private KnowledgeChunk createChunk(String chunkId, String docId, String text) {
        return createChunkWithTenant(chunkId, docId, text, "tenant-1");
    }

    private KnowledgeChunk createChunkWithTenant(String chunkId, String docId, String text, String tenantId) {
        return new KnowledgeChunk(
                chunkId, docId, "src-1", tenantId,
                text, ChunkLocation.of(0, ""), "",
                DocumentClassification.PUBLIC, "en", null, Map.of());
    }
}
