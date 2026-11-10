package io.github.regalpine.agentforge.a2a;

import java.time.Instant;
import java.util.Map;
import java.util.Objects;

/**
 * Artifact reference.
 * AF-ENHANCE-10 §37-38: Artifact.
 *
 * <p>Actual content should be managed
 * through Workspace / Artifact Store
 * / Object Store. Large files should
 * not be embedded in messages.
 */
public record ArtifactReference(
    String artifactId,
    String digest,
    String contentType,
    long size,
    String origin,
    Instant createdAt,
    String classification,
    Map<String, Object> metadata
) {

    public ArtifactReference {
        Objects.requireNonNull(
            artifactId, "artifactId"
        );

        digest = digest == null
            ? "" : digest;
        contentType = contentType == null
            ? "application/octet-stream"
            : contentType;
        origin = origin == null
            ? "" : origin;
        createdAt = createdAt == null
            ? Instant.now() : createdAt;
        classification = classification == null
            ? "internal" : classification;
        metadata = metadata == null
            ? Map.of() : Map.copyOf(metadata);
    }

    /**
     * Create a minimal reference.
     */
    public static ArtifactReference of(
        String artifactId,
        String contentType
    ) {
        return new ArtifactReference(
            artifactId, null,
            contentType, 0,
            null, null, null, null
        );
    }

    /**
     * Create with digest.
     */
    public static ArtifactReference of(
        String artifactId,
        String digest,
        String contentType,
        long size
    ) {
        return new ArtifactReference(
            artifactId, digest,
            contentType, size,
            null, null, null, null
        );
    }
}
