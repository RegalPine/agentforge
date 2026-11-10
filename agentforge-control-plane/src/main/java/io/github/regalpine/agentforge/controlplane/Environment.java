package io.github.regalpine.agentforge.controlplane;

import java.util.Map;

/**
 * Environment definition.
 * AF-ENHANCE-14 §16: Environment.
 *
 * <p>Environments are isolated:
 * DEV ≠ TEST ≠ STAGING ≠ PRODUCTION.
 */
public record Environment(
    EnvironmentId envId,
    String type,
    Map<String, String> config,
    Map<String, String> metadata
) {

    public Environment {
        if (envId == null) {
            throw new IllegalArgumentException(
                "envId required");
        }
        config = config == null
            ? Map.of() : Map.copyOf(config);
        metadata = metadata == null
            ? Map.of() : Map.copyOf(metadata);
    }

    /**
     * Create simple environment.
     */
    public static Environment of(
        EnvironmentId envId
    ) {
        return new Environment(
            envId, envId.value(),
            Map.of(), Map.of()
        );
    }
}
