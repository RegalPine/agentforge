package io.github.regalpine.agentforge.controlplane;

import java.util.Map;

/**
 * Resource quota.
 * AF-ENHANCE-14 §24: Resource Quota.
 *
 * <p>Budget hierarchy:
 * Tenant → Application → Deployment
 * → Execution → Skill/Tool/Model.
 */
public record ResourceQuota(
    String quotaId,
    QuotaScope scope,
    Map<String, Long> limits,
    Map<String, Long> used
) {

    public ResourceQuota {
        if (quotaId == null
            || quotaId.isBlank()) {
            throw new IllegalArgumentException(
                "quotaId required");
        }
        if (scope == null) {
            throw new IllegalArgumentException(
                "scope required");
        }
        limits = limits == null
            ? Map.of() : Map.copyOf(limits);
        used = used == null
            ? Map.of() : Map.copyOf(used);
    }

    /**
     * Check if a resource is within
     * quota limits.
     */
    public boolean isWithinLimit(
        String resource
    ) {
        Long limit = limits.get(resource);
        Long current = used.get(resource);
        if (limit == null) return true;
        if (current == null) return true;
        return current < limit;
    }

    /**
     * Create minimal quota.
     */
    public static ResourceQuota of(
        String quotaId,
        QuotaScope scope
    ) {
        return new ResourceQuota(
            quotaId, scope,
            Map.of(), Map.of()
        );
    }
}
