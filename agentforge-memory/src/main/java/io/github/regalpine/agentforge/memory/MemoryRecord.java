package io.github.regalpine.agentforge.memory;

import io.github.regalpine.agentforge.security.DataClassification;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Core memory record.
 * AE-08 §9: Memory Record.
 *
 * <p>Immutable value representing a
 * persisted memory. Records carry their
 * full provenance, confidence, lifecycle
 * status and security classification.
 */
public record MemoryRecord(
    String memoryId,
    String tenantId,
    String subjectId,
    MemoryType type,
    MemoryScope scope,
    String content,
    Map<String, Object> attributes,
    Instant createdAt,
    Instant updatedAt,
    Instant expiresAt,
    MemoryStatus status,
    MemorySource source,
    MemoryConfidence confidence,
    MemoryImportance importance,
    MemoryProvenance provenance,
    DataClassification classification,
    long version
) {

    public MemoryRecord {
        Objects.requireNonNull(memoryId, "memoryId");
        Objects.requireNonNull(tenantId, "tenantId");
        Objects.requireNonNull(subjectId, "subjectId");
        Objects.requireNonNull(type, "type");
        Objects.requireNonNull(scope, "scope");
        Objects.requireNonNull(content, "content");

        if (content.isBlank()) {
            throw new IllegalArgumentException(
                "Memory content cannot be blank"
            );
        }

        attributes =
            attributes == null
                ? Map.of()
                : Map.copyOf(attributes);

        createdAt =
            createdAt == null
                ? Instant.now()
                : createdAt;

        updatedAt =
            updatedAt == null
                ? createdAt
                : updatedAt;

        status =
            status == null
                ? MemoryStatus.CANDIDATE
                : status;

        source =
            source == null
                ? MemorySource.SYSTEM
                : source;

        confidence =
            confidence == null
                ? MemoryConfidence.high(
                    ConfidenceBasis.MANUAL
                )
                : confidence;

        importance =
            importance == null
                ? MemoryImportance.NORMAL
                : importance;

        provenance =
            provenance == null
                ? MemoryProvenance.empty()
                : provenance;

        classification =
            classification == null
                ? DataClassification.INTERNAL
                : classification;

        if (version < 0) {
            version = 0;
        }
    }

    /**
     * Check if this memory has expired
     * relative to the given instant.
     */
    public boolean isExpired(Instant now) {
        return expiresAt != null
            && now.isAfter(expiresAt);
    }

    /**
     * Check if this memory is currently
     * active and available for retrieval.
     */
    public boolean isRetrievable(Instant now) {
        return status == MemoryStatus.ACTIVE
            && !isExpired(now);
    }

    /**
     * Create a copy with updated status.
     */
    public MemoryRecord withStatus(
        MemoryStatus newStatus
    ) {
        return new MemoryRecord(
            memoryId, tenantId, subjectId,
            type, scope, content, attributes,
            createdAt, Instant.now(), expiresAt,
            newStatus, source, confidence,
            importance, provenance,
            classification, version
        );
    }

    /**
     * Create a copy with incremented version.
     */
    public MemoryRecord withVersion(
        long newVersion
    ) {
        return new MemoryRecord(
            memoryId, tenantId, subjectId,
            type, scope, content, attributes,
            createdAt, Instant.now(), expiresAt,
            status, source, confidence,
            importance, provenance,
            classification, newVersion
        );
    }

    /**
     * Builder for constructing MemoryRecords
     * with a fluent API.
     */
    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {

        private String memoryId;
        private String tenantId;
        private String subjectId;
        private MemoryType type;
        private MemoryScope scope;
        private String content;
        private Map<String, Object> attributes;
        private Instant createdAt;
        private Instant updatedAt;
        private Instant expiresAt;
        private MemoryStatus status;
        private MemorySource source;
        private MemoryConfidence confidence;
        private MemoryImportance importance;
        private MemoryProvenance provenance;
        private DataClassification classification;
        private long version;

        private Builder() {}

        public Builder memoryId(String v) {
            this.memoryId = v; return this;
        }

        public Builder tenantId(String v) {
            this.tenantId = v; return this;
        }

        public Builder subjectId(String v) {
            this.subjectId = v; return this;
        }

        public Builder type(MemoryType v) {
            this.type = v; return this;
        }

        public Builder scope(MemoryScope v) {
            this.scope = v; return this;
        }

        public Builder content(String v) {
            this.content = v; return this;
        }

        public Builder attributes(Map<String, Object> v) {
            this.attributes = v; return this;
        }

        public Builder createdAt(Instant v) {
            this.createdAt = v; return this;
        }

        public Builder updatedAt(Instant v) {
            this.updatedAt = v; return this;
        }

        public Builder expiresAt(Instant v) {
            this.expiresAt = v; return this;
        }

        public Builder status(MemoryStatus v) {
            this.status = v; return this;
        }

        public Builder source(MemorySource v) {
            this.source = v; return this;
        }

        public Builder confidence(MemoryConfidence v) {
            this.confidence = v; return this;
        }

        public Builder importance(MemoryImportance v) {
            this.importance = v; return this;
        }

        public Builder provenance(MemoryProvenance v) {
            this.provenance = v; return this;
        }

        public Builder classification(DataClassification v) {
            this.classification = v; return this;
        }

        public Builder version(long v) {
            this.version = v; return this;
        }

        public MemoryRecord build() {
            return new MemoryRecord(
                memoryId, tenantId, subjectId,
                type, scope, content, attributes,
                createdAt, updatedAt, expiresAt,
                status, source, confidence,
                importance, provenance,
                classification, version
            );
        }
    }
}
