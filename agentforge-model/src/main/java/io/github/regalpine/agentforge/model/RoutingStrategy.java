package io.github.regalpine.agentforge.model;

/**
 * Routing strategy —
 * AF-ENHANCE-09 §25.
 *
 * <p>V1.0 routing strategies:
 * STATIC, PRIORITY, WEIGHTED,
 * CAPABILITY, FAILOVER,
 * LATENCY_AWARE, COST_AWARE,
 * QUOTA_AWARE, LOAD_AWARE,
 * REGION_AWARE
 */
public enum RoutingStrategy {

    /**
     * Fixed model selection.
     */
    STATIC,

    /**
     * Priority-based fallback.
     */
    PRIORITY,

    /**
     * Weighted distribution.
     */
    WEIGHTED,

    /**
     * Match required capabilities.
     */
    CAPABILITY,

    /**
     * Failover on error.
     */
    FAILOVER,

    /**
     * Prefer low latency.
     */
    LATENCY_AWARE,

    /**
     * Prefer low cost.
     */
    COST_AWARE,

    /**
     * Respect quota limits.
     */
    QUOTA_AWARE,

    /**
     * Balance by load.
     */
    LOAD_AWARE,

    /**
     * Region-based selection.
     */
    REGION_AWARE
}
