package io.github.regalpine.agentforge.controlplane;

import java.util.List;
import java.util.Map;
import java.time.Instant;

/**
 * In-memory RuntimeInstance.
 * AF-ENHANCE-14 §11: Runtime Instance.
 */
public record InMemoryRuntimeInstance(
    String instanceId,
    DeploymentId deploymentRef,
    String revisionRef,
    InstanceStatus instanceStatus,
    RuntimeHealth instanceHealth,
    Instant startedAt,
    Map<String, String> metadata
) implements RuntimeInstance {

    public InMemoryRuntimeInstance {
        if (instanceId == null
            || instanceId.isBlank()) {
            throw new IllegalArgumentException(
                "instanceId required");
        }
        if (deploymentRef == null) {
            throw new IllegalArgumentException(
                "deploymentRef required");
        }
        if (instanceStatus == null) {
            instanceStatus =
                InstanceStatus.CREATED;
        }
        if (instanceHealth == null) {
            instanceHealth =
                RuntimeHealth.unknown();
        }
        metadata = metadata == null
            ? Map.of() : Map.copyOf(metadata);
        if (startedAt == null) {
            startedAt = Instant.now();
        }
    }

    @Override
    public String id() {
        return instanceId;
    }

    @Override
    public DeploymentId deployment() {
        return deploymentRef;
    }

    @Override
    public String revisionId() {
        return revisionRef;
    }

    @Override
    public InstanceStatus status() {
        return instanceStatus;
    }

    @Override
    public RuntimeHealth health() {
        return instanceHealth;
    }

    /**
     * Create a new instance.
     */
    public static InMemoryRuntimeInstance of(
        String instanceId,
        DeploymentId deploymentRef,
        String revisionRef
    ) {
        return new InMemoryRuntimeInstance(
            instanceId, deploymentRef,
            revisionRef,
            InstanceStatus.CREATED,
            RuntimeHealth.unknown(),
            Instant.now(),
            Map.of()
        );
    }

    /**
     * Start this instance.
     */
    public InMemoryRuntimeInstance start() {
        return new InMemoryRuntimeInstance(
            instanceId, deploymentRef,
            revisionRef,
            InstanceStatus.RUNNING,
            new RuntimeHealth(
                HealthStatus.HEALTHY,
                HealthStatus.HEALTHY,
                10, Instant.now()
            ),
            startedAt, metadata
        );
    }

    /**
     * Stop this instance.
     */
    public InMemoryRuntimeInstance stop() {
        return new InMemoryRuntimeInstance(
            instanceId, deploymentRef,
            revisionRef,
            InstanceStatus.STOPPED,
            instanceHealth,
            startedAt, metadata
        );
    }
}
