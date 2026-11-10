package io.github.regalpine.agentforge.controlplane;

import java.util.List;
import java.util.Map;
import java.util.ArrayList;
import java.util.Collections;

/**
 * Runtime pool: multiple instances
 * for a deployment.
 * AF-ENHANCE-14 §13: Runtime Pool.
 */
public record RuntimePool(
    String poolId,
    DeploymentId deploymentId,
    List<RuntimeInstance> instances,
    int minReplicas,
    int maxReplicas,
    Map<String, String> metadata
) {

    public RuntimePool {
        if (poolId == null
            || poolId.isBlank()) {
            throw new IllegalArgumentException(
                "poolId required");
        }
        if (deploymentId == null) {
            throw new IllegalArgumentException(
                "deploymentId required");
        }
        instances = instances == null
            ? List.of()
            : List.copyOf(instances);
        metadata = metadata == null
            ? Map.of() : Map.copyOf(metadata);
    }

    /**
     * Create minimal pool.
     */
    public static RuntimePool of(
        String poolId,
        DeploymentId deploymentId
    ) {
        return new RuntimePool(
            poolId, deploymentId,
            List.of(), 1, 1,
            Map.of()
        );
    }

    /**
     * Count available instances.
     */
    public int availableCount() {
        int count = 0;
        for (var inst : instances) {
            if (inst.status()
                .isAvailable()) {
                count++;
            }
        }
        return count;
    }

    /**
     * Check if pool is healthy.
     */
    public boolean isHealthy() {
        return availableCount()
            >= minReplicas;
    }
}
