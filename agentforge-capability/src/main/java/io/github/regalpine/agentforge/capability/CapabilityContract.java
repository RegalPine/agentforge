package io.github.regalpine.agentforge.capability;

import java.time.Duration;
import java.util.Set;

/**
 * Capability contract.
 * AE-02 §16, §27, §45: defines timeout,
 * idempotency, permissions, type, side effect
 * and retry policy for a capability.
 */
public record CapabilityContract(
    Duration timeout,
    boolean idempotent,
    Set<String> requiredPermissions,
    CapabilityType type,
    SideEffectClassification sideEffect,
    RetryPolicy retryPolicy
) {

    public CapabilityContract {
        timeout =
            timeout == null
                ? Duration.ofSeconds(30)
                : timeout;

        if (timeout.isZero() || timeout.isNegative()) {
            throw new IllegalArgumentException(
                "Capability timeout must be positive"
            );
        }

        requiredPermissions =
            requiredPermissions == null
                ? Set.of()
                : Set.copyOf(requiredPermissions);

        type =
            type == null
                ? CapabilityType.TOOL
                : type;

        sideEffect =
            sideEffect == null
                ? SideEffectClassification.READ_ONLY
                : sideEffect;

        retryPolicy =
            retryPolicy == null
                ? RetryPolicy.none()
                : retryPolicy;
    }

    public static CapabilityContract defaults() {
        return new CapabilityContract(
            Duration.ofSeconds(30),
            false,
            Set.of(),
            CapabilityType.TOOL,
            SideEffectClassification.READ_ONLY,
            RetryPolicy.none()
        );
    }
}
