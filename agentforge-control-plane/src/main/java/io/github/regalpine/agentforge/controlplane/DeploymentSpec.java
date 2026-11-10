package io.github.regalpine.agentforge.controlplane;

import java.util.Map;

/**
 * Deployment specification.
 * AF-ENHANCE-14 §8: Deployment Spec.
 */
public record DeploymentSpec(
    String agentVersion,
    String agentDigest,
    int minReplicas,
    int maxReplicas,
    DeploymentStrategy strategy,
    String policyReference,
    Map<String, String> resources,
    Map<String, String> extensions
) {

    public DeploymentSpec {
        if (agentVersion == null
            || agentVersion.isBlank()) {
            throw new IllegalArgumentException(
                "agentVersion required");
        }
        resources = resources == null
            ? Map.of() : Map.copyOf(resources);
        extensions = extensions == null
            ? Map.of() : Map.copyOf(extensions);
    }

    /**
     * Create minimal spec.
     */
    public static DeploymentSpec of(
        String agentVersion
    ) {
        return new DeploymentSpec(
            agentVersion, null,
            1, 1,
            DeploymentStrategy.ROLLING,
            null, Map.of(), Map.of()
        );
    }
}
