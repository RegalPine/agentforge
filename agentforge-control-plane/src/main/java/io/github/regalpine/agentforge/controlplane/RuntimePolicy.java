package io.github.regalpine.agentforge.controlplane;

import java.time.Duration;
import java.util.Map;

/**
 * Runtime policy for execution
 * governance.
 * AF-ENHANCE-14 §60: Runtime Policy.
 */
public record RuntimePolicy(
    String policyId,
    int maxConcurrentExecutions,
    Duration timeout,
    int maxRetries,
    Map<String, Long> resourceLimits,
    Map<String, String> extensions
) {

    public RuntimePolicy {
        if (policyId == null
            || policyId.isBlank()) {
            throw new IllegalArgumentException(
                "policyId required");
        }
        resourceLimits = resourceLimits == null
            ? Map.of()
            : Map.copyOf(resourceLimits);
        extensions = extensions == null
            ? Map.of()
            : Map.copyOf(extensions);
    }

    /**
     * Create default policy.
     */
    public static RuntimePolicy defaults(
        String policyId
    ) {
        return new RuntimePolicy(
            policyId,
            10,
            Duration.ofSeconds(300),
            3,
            Map.of(), Map.of()
        );
    }
}
