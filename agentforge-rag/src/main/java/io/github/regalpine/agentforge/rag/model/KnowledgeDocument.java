package io.github.regalpine.agentforge.rag.model;

import java.time.Instant;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Knowledge document.
 * AE-09 §9: Document Model.
 *
 * <p>A manageable knowledge resource unit within a Knowledge Source.
 * Supports versioning, classification, and validity periods.
 */
public final class KnowledgeDocument {

    private final String documentId;
    private final String sourceId;
    private final String tenantId;
    private final String externalId;
    private final String title;
    private final String mimeType;
    private final String version;
    private final String checksum;
    private final DocumentStatus status;
    private final DocumentClassification classification;
    private final KnowledgeAuthority authority;
    private final Instant validFrom;
    private final Instant validTo;
    private final Instant publishedAt;
    private final Instant updatedAt;
    private final Instant indexedAt;
    private final Map<String, Object> metadata;

    public KnowledgeDocument(
            String documentId,
            String sourceId,
            String tenantId,
            String externalId,
            String title,
            String mimeType,
            String version,
            String checksum,
            DocumentStatus status,
            DocumentClassification classification,
            KnowledgeAuthority authority,
            Instant validFrom,
            Instant validTo,
            Instant publishedAt,
            Instant updatedAt,
            Instant indexedAt,
            Map<String, Object> metadata) {
        this.documentId = Objects.requireNonNull(documentId, "documentId");
        this.sourceId = Objects.requireNonNull(sourceId, "sourceId");
        this.tenantId = Objects.requireNonNull(tenantId, "tenantId");
        this.externalId = externalId == null ? "" : externalId;
        this.title = Objects.requireNonNull(title, "title");
        this.mimeType = mimeType == null ? "text/plain" : mimeType;
        this.version = Objects.requireNonNull(version, "version");
        this.checksum = checksum == null ? "" : checksum;
        this.status = Objects.requireNonNull(status, "status");
        this.classification = Objects.requireNonNull(classification, "classification");
        this.authority = Objects.requireNonNull(authority, "authority");
        this.validFrom = validFrom;
        this.validTo = validTo;
        this.publishedAt = publishedAt;
        this.updatedAt = updatedAt;
        this.indexedAt = indexedAt;
        this.metadata = metadata == null ? Map.of() : Map.copyOf(metadata);
    }

    public String documentId() { return documentId; }
    public String sourceId() { return sourceId; }
    public String tenantId() { return tenantId; }
    public String externalId() { return externalId; }
    public String title() { return title; }
    public String mimeType() { return mimeType; }
    public String version() { return version; }
    public String checksum() { return checksum; }
    public DocumentStatus status() { return status; }
    public DocumentClassification classification() { return classification; }
    public KnowledgeAuthority authority() { return authority; }
    public Instant validFrom() { return validFrom; }
    public Instant validTo() { return validTo; }
    public Instant publishedAt() { return publishedAt; }
    public Instant updatedAt() { return updatedAt; }
    public Instant indexedAt() { return indexedAt; }
    public Map<String, Object> metadata() { return metadata; }

    /**
     * Check if this document is currently valid (within validity period).
     */
    public boolean isValidAt(Instant instant) {
        if (validFrom != null && instant.isBefore(validFrom)) return false;
        if (validTo != null && instant.isAfter(validTo)) return false;
        return true;
    }

    public Optional<String> metadataValue(String key) {
        Object val = metadata.get(key);
        return val != null ? Optional.of(val.toString()) : Optional.empty();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof KnowledgeDocument that)) return false;
        return documentId.equals(that.documentId);
    }

    @Override
    public int hashCode() {
        return documentId.hashCode();
    }

    @Override
    public String toString() {
        return "KnowledgeDocument{id=" + documentId + ", title=" + title + ", v=" + version + ", status=" + status + "}";
    }
}
