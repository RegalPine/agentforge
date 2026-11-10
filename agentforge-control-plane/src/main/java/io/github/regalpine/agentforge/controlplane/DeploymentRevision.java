package io.github.regalpine.agentforge.controlplane;

import java.time.Instant;

/**
 * Immutable deployment revision.
 * AF-ENHANCE-14 §9: Deployment Revision.
 *
 * <p>Each deployment change produces
 * a new Revision. Revisions are
 * immutable.
 */
public record DeploymentRevision(
    String revisionId,
    DeploymentId deploymentId,
    String agentVersion,
    String agentDigest,
    String configDigest,
    Instant createdAt
) {

    public DeploymentRevision {
        if (revisionId == null
            || revisionId.isBlank()) {
            throw new IllegalArgumentException(
                "revisionId required");
        }
        if (deploymentId == null) {
            throw new IllegalArgumentException(
                "deploymentId required");
        }
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }

    /**
     * Create a new revision.
     */
    public static DeploymentRevision of(
        String revisionId,
        DeploymentId deploymentId,
        String agentVersion
    ) {
        return new DeploymentRevision(
            revisionId, deploymentId,
            agentVersion, null, null,
            Instant.now()
        );
    }
}
