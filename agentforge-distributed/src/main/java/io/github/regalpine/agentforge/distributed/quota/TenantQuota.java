package io.github.regalpine.agentforge.distributed.quota;

import java.util.Objects;

/**
 * TenantQuota — AF-RUNTIME-01 §25-26.
 *
 * Defines resource limits for a tenant.
 * Supports hierarchical quota:
 * Global → Tenant → Application → Agent → Execution.
 *
 * Effective limit = min(all levels).
 */
public record TenantQuota(

    String tenantId,

    int maxConcurrentExecutions,

    int maxQueuedExecutions,

    long maxModelCallsPerDay,

    long maxToolCallsPerDay,

    long maxTokensPerDay,

    long maxDurationMs,

    long maxCostCentsPerDay
) {

    public TenantQuota {
        Objects.requireNonNull(tenantId, "tenantId");

        if (maxConcurrentExecutions < 0) {
            throw new IllegalArgumentException(
                "maxConcurrentExecutions must be >= 0");
        }
        if (maxQueuedExecutions < 0) {
            throw new IllegalArgumentException(
                "maxQueuedExecutions must be >= 0");
        }
    }

    /**
     * Create a permissive quota for testing.
     */
    public static TenantQuota unlimited(String tenantId) {
        return new TenantQuota(
            tenantId,
            Integer.MAX_VALUE,
            Integer.MAX_VALUE,
            Long.MAX_VALUE,
            Long.MAX_VALUE,
            Long.MAX_VALUE,
            Long.MAX_VALUE,
            Long.MAX_VALUE
        );
    }

    /**
     * Create a default quota with reasonable limits.
     */
    public static TenantQuota defaults(String tenantId) {
        return new TenantQuota(
            tenantId,
            100,
            1000,
            1_000_000,
            500_000,
            10_000_000,
            3_600_000,
            10_000
        );
    }

    /**
     * Merge with another quota, taking the minimum
     * of each limit (hierarchical quota §26).
     */
    public TenantQuota mergeWith(TenantQuota other) {
        if (other == null) return this;
        return new TenantQuota(
            tenantId,
            Math.min(maxConcurrentExecutions,
                other.maxConcurrentExecutions),
            Math.min(maxQueuedExecutions,
                other.maxQueuedExecutions),
            Math.min(maxModelCallsPerDay,
                other.maxModelCallsPerDay),
            Math.min(maxToolCallsPerDay,
                other.maxToolCallsPerDay),
            Math.min(maxTokensPerDay,
                other.maxTokensPerDay),
            Math.min(maxDurationMs,
                other.maxDurationMs),
            Math.min(maxCostCentsPerDay,
                other.maxCostCentsPerDay)
        );
    }
}
