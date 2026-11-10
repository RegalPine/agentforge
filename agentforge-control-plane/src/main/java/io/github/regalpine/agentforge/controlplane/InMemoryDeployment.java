package io.github.regalpine.agentforge.controlplane;

import java.util.List;
import java.util.Map;
import java.time.Instant;

/**
 * In-memory Deployment record.
 * AF-ENHANCE-14 §6: Deployment.
 *
 * <p>Simple record implementing
 * the Deployment interface for
 * testing and development.
 */
public record InMemoryDeployment(
    DeploymentId id,
    String agentReference,
    EnvironmentId environment,
    DeploymentSpec spec,
    DeploymentStatus status,
    List<DeploymentRevision> revisions,
    Instant createdAt,
    Map<String, String> metadata
) implements Deployment {

    public InMemoryDeployment {
        if (id == null) {
            throw new IllegalArgumentException(
                "id required");
        }
        if (agentReference == null
            || agentReference.isBlank()) {
            throw new IllegalArgumentException(
                "agentReference required");
        }
        if (environment == null) {
            throw new IllegalArgumentException(
                "environment required");
        }
        if (spec == null) {
            throw new IllegalArgumentException(
                "spec required");
        }
        if (status == null) {
            status = DeploymentStatus.CREATED;
        }
        revisions = revisions == null
            ? List.of()
            : List.copyOf(revisions);
        metadata = metadata == null
            ? Map.of() : Map.copyOf(metadata);
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }

    /**
     * Create a new deployment.
     */
    public static InMemoryDeployment of(
        DeploymentId id,
        String agentReference,
        EnvironmentId environment,
        DeploymentSpec spec
    ) {
        return new InMemoryDeployment(
            id, agentReference,
            environment, spec,
            DeploymentStatus.CREATED,
            List.of(),
            Instant.now(),
            Map.of()
        );
    }

    /**
     * Activate this deployment.
     */
    public InMemoryDeployment activate() {
        return new InMemoryDeployment(
            id, agentReference,
            environment, spec,
            DeploymentStatus.ACTIVE,
            revisions, createdAt,
            metadata
        );
    }

    /**
     * Stop this deployment.
     */
    public InMemoryDeployment stop() {
        return new InMemoryDeployment(
            id, agentReference,
            environment, spec,
            DeploymentStatus.STOPPED,
            revisions, createdAt,
            metadata
        );
    }

    /**
     * Add a revision.
     */
    public InMemoryDeployment addRevision(
        DeploymentRevision rev
    ) {
        var updated = new java.util.ArrayList<>(
            revisions);
        updated.add(rev);
        return new InMemoryDeployment(
            id, agentReference,
            environment, spec,
            status,
            List.copyOf(updated),
            createdAt, metadata
        );
    }
}
