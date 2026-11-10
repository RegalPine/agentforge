package io.github.regalpine.agentforge.workspace;

import io.github.regalpine.agentforge.security.DataClassification;

import java.time.Instant;
import java.util.Map;
import java.util.Objects;

/**
 * Artifact record.
 * AF-ENHANCE-07 §14-18: Artifact.
 *
 * <p>An independently referenceable
 * execution output with identity,
 * digest, lifecycle and metadata.
 */
public record Artifact(
    ArtifactId artifactId,
    String workspaceId,
    String executionId,
    String agentId,
    String name,
    String contentType,
    long size,
    String digest,
    ArtifactStatus status,
    DataClassification classification,
    Map<String, Object> metadata,
    Instant createdAt,
    Instant updatedAt
) {

    public Artifact {
        Objects.requireNonNull(
            artifactId, "artifactId"
        );
        Objects.requireNonNull(
            name, "name"
        );

        workspaceId =
            workspaceId == null
                ? "" : workspaceId;
        executionId =
            executionId == null
                ? "" : executionId;
        agentId =
            agentId == null
                ? "" : agentId;
        contentType =
            contentType == null
                ? "application/octet-stream"
                : contentType;
        digest =
            digest == null
                ? "" : digest;

        status =
            status == null
                ? ArtifactStatus.CREATED
                : status;
        classification =
            classification == null
                ? DataClassification.INTERNAL
                : classification;

        metadata =
            metadata == null
                ? Map.of()
                : Map.copyOf(metadata);

        createdAt =
            createdAt == null
                ? Instant.now()
                : createdAt;
        updatedAt =
            updatedAt == null
                ? createdAt
                : updatedAt;
    }

    /**
     * Create a copy with updated status.
     */
    public Artifact withStatus(
        ArtifactStatus newStatus
    ) {
        return new Artifact(
            artifactId, workspaceId,
            executionId, agentId,
            name, contentType, size,
            digest, newStatus,
            classification, metadata,
            createdAt, Instant.now()
        );
    }

    /**
     * Builder for artifact creation.
     */
    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {

        private ArtifactId artifactId;
        private String workspaceId;
        private String executionId;
        private String agentId;
        private String name;
        private String contentType;
        private long size;
        private String digest;
        private ArtifactStatus status;
        private DataClassification classification;
        private Map<String, Object> metadata;
        private Instant createdAt;

        private Builder() {}

        public Builder artifactId(ArtifactId v) {
            this.artifactId = v;
            return this;
        }

        public Builder workspaceId(String v) {
            this.workspaceId = v;
            return this;
        }

        public Builder executionId(String v) {
            this.executionId = v;
            return this;
        }

        public Builder agentId(String v) {
            this.agentId = v;
            return this;
        }

        public Builder name(String v) {
            this.name = v;
            return this;
        }

        public Builder contentType(String v) {
            this.contentType = v;
            return this;
        }

        public Builder size(long v) {
            this.size = v;
            return this;
        }

        public Builder digest(String v) {
            this.digest = v;
            return this;
        }

        public Builder status(ArtifactStatus v) {
            this.status = v;
            return this;
        }

        public Builder classification(DataClassification v) {
            this.classification = v;
            return this;
        }

        public Builder metadata(Map<String, Object> v) {
            this.metadata = v;
            return this;
        }

        public Builder createdAt(Instant v) {
            this.createdAt = v;
            return this;
        }

        public Artifact build() {
            return new Artifact(
                artifactId, workspaceId,
                executionId, agentId,
                name, contentType, size,
                digest, status,
                classification, metadata,
                createdAt, null
            );
        }
    }
}
