package io.github.regalpine.agentforge.sdk;

import java.util.Map;
import java.util.Objects;

/**
 * Reference to a registered Policy.
 * AF-ENHANCE-01 §14: AgentBuilder.policy().
 * AF-ENHANCE-03: HITL Runtime (X2 phase).
 *
 * <p>Lightweight reference that identifies
 * a Policy by ID and version.
 */
public record PolicyRef(
    String id,
    String version
) {

    public PolicyRef {
        Objects.requireNonNull(id, "id");
    }

    /**
     * Create a policy reference with
     * just an ID (default version).
     */
    public static PolicyRef of(String id) {
        return new PolicyRef(id, "1.0.0");
    }
}
