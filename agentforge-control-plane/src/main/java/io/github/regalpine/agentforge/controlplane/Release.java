package io.github.regalpine.agentforge.controlplane;

import java.time.Instant;
import java.util.Map;

/**
 * Production release.
 * AF-ENHANCE-14 §46: Release.
 *
 * <p>A release represents an approved
 * Agent runtime artifact entering
 * an environment.
 */
public record Release(
    String releaseId,
    DeploymentId deploymentId,
    String revisionId,
    EnvironmentId environment,
    ReleaseStatus status,
    String agentDigest,
    Instant createdAt,
    Map<String, String> metadata
) {

    public Release {
        if (releaseId == null
            || releaseId.isBlank()) {
            throw new IllegalArgumentException(
                "releaseId required");
        }
        if (deploymentId == null) {
            throw new IllegalArgumentException(
                "deploymentId required");
        }
        if (environment == null) {
            throw new IllegalArgumentException(
                "environment required");
        }
        if (status == null) {
            status = ReleaseStatus.CREATED;
        }
        metadata = metadata == null
            ? Map.of() : Map.copyOf(metadata);
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }

    /**
     * Create a new release.
     */
    public static Release of(
        String releaseId,
        DeploymentId deploymentId,
        String revisionId,
        EnvironmentId environment
    ) {
        return new Release(
            releaseId, deploymentId,
            revisionId, environment,
            ReleaseStatus.CREATED,
            null, Instant.now(),
            Map.of()
        );
    }

    /**
     * Approve this release.
     */
    public Release approve() {
        return new Release(
            releaseId, deploymentId,
            revisionId, environment,
            ReleaseStatus.APPROVED,
            agentDigest, createdAt,
            metadata
        );
    }

    /**
     * Deploy this release.
     */
    public Release deploy() {
        return new Release(
            releaseId, deploymentId,
            revisionId, environment,
            ReleaseStatus.ACTIVE,
            agentDigest, createdAt,
            metadata
        );
    }

    /**
     * Rollback this release.
     */
    public Release rollback() {
        return new Release(
            releaseId, deploymentId,
            revisionId, environment,
            ReleaseStatus.ROLLED_BACK,
            agentDigest, createdAt,
            metadata
        );
    }
}
