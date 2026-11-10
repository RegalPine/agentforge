package io.github.regalpine.agentforge.controlplane;

import java.time.Instant;

/**
 * Rollout: deployment of a specific
 * revision using a strategy.
 * AF-ENHANCE-14 §39: Deployment
 * Strategy.
 */
public record Rollout(
    String rolloutId,
    DeploymentId deploymentId,
    String revisionId,
    DeploymentStrategy strategy,
    RolloutStatus status,
    int progress,
    Instant createdAt
) {

    public Rollout {
        if (rolloutId == null
            || rolloutId.isBlank()) {
            throw new IllegalArgumentException(
                "rolloutId required");
        }
        if (deploymentId == null) {
            throw new IllegalArgumentException(
                "deploymentId required");
        }
        if (strategy == null) {
            strategy = DeploymentStrategy
                .ROLLING;
        }
        if (status == null) {
            status = RolloutStatus.CREATED;
        }
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }

    /**
     * Create a new rollout.
     */
    public static Rollout of(
        String rolloutId,
        DeploymentId deploymentId,
        String revisionId,
        DeploymentStrategy strategy
    ) {
        return new Rollout(
            rolloutId, deploymentId,
            revisionId, strategy,
            RolloutStatus.CREATED,
            0, Instant.now()
        );
    }

    /**
     * Advance rollout progress.
     */
    public Rollout advance(
        int newProgress
    ) {
        return new Rollout(
            rolloutId, deploymentId,
            revisionId, strategy,
            newProgress >= 100
                ? RolloutStatus.COMPLETED
                : RolloutStatus.IN_PROGRESS,
            newProgress, createdAt
        );
    }

    /**
     * Rollback this rollout.
     */
    public Rollout rollback() {
        return new Rollout(
            rolloutId, deploymentId,
            revisionId, strategy,
            RolloutStatus.ROLLED_BACK,
            progress, createdAt
        );
    }
}
