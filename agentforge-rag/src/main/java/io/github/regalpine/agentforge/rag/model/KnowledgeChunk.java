package io.github.regalpine.agentforge.rag.model;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Knowledge chunk — the minimum semantic retrieval unit.
 * AE-09 §17: Chunk.
 *
 * <p>Each chunk belongs to a document, has text content,
 * a location within the document, and metadata including
 * tenant, source, classification, etc.
 */
public final class KnowledgeChunk {

    private final String chunkId;
    private final String documentId;
    private final String sourceId;
    private final String tenantId;
    private final String text;
    private final ChunkLocation location;
    private final String checksum;
    private final DocumentClassification classification;
    private final String language;
    private final float[] embedding;
    private final Map<String, Object> metadata;

    public KnowledgeChunk(
            String chunkId,
            String documentId,
            String sourceId,
            String tenantId,
            String text,
            ChunkLocation location,
            String checksum,
            DocumentClassification classification,
            String language,
            float[] embedding,
            Map<String, Object> metadata) {
        this.chunkId = Objects.requireNonNull(chunkId, "chunkId");
        this.documentId = Objects.requireNonNull(documentId, "documentId");
        this.sourceId = Objects.requireNonNull(sourceId, "sourceId");
        this.tenantId = Objects.requireNonNull(tenantId, "tenantId");
        this.text = Objects.requireNonNull(text, "text");
        this.location = Objects.requireNonNull(location, "location");
        this.checksum = checksum == null ? "" : checksum;
        this.classification = Objects.requireNonNull(classification, "classification");
        this.language = language == null ? "en" : language;
        this.embedding = embedding;
        this.metadata = metadata == null ? Map.of() : Map.copyOf(metadata);
    }

    public String chunkId() { return chunkId; }
    public String documentId() { return documentId; }
    public String sourceId() { return sourceId; }
    public String tenantId() { return tenantId; }
    public String text() { return text; }
    public ChunkLocation location() { return location; }
    public String checksum() { return checksum; }
    public DocumentClassification classification() { return classification; }
    public String language() { return language; }
    public float[] embedding() { return embedding; }
    public Map<String, Object> metadata() { return metadata; }

    public boolean hasEmbedding() {
        return embedding != null && embedding.length > 0;
    }

    public int embeddingDimension() {
        return embedding == null ? 0 : embedding.length;
    }

    public Optional<Object> metadataValue(String key) {
        return Optional.ofNullable(metadata.get(key));
    }

    /**
     * Estimated token count (~4 chars per token).
     */
    public int estimatedTokens() {
        return Math.max(1, text.length() / 4);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof KnowledgeChunk that)) return false;
        return chunkId.equals(that.chunkId);
    }

    @Override
    public int hashCode() {
        return chunkId.hashCode();
    }

    @Override
    public String toString() {
        return "KnowledgeChunk{id=" + chunkId + ", doc=" + documentId
                + ", tokens=" + estimatedTokens() + ", loc=" + location + "}";
    }
}
