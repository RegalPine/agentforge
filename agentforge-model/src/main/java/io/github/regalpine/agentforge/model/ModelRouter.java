package io.github.regalpine.agentforge.model;

import java.util.List;

/**
 * Model router —
 * AF-ENHANCE-09 §24.
 *
 * <p>Unified routing entry point.
 * Selects the best model from
 * candidates based on strategy,
 * policy, health, quota.
 *
 * <p>Routing pipeline:
 * Request → Capability Filter →
 * Policy Filter → Availability
 * Filter → Quota Filter →
 * Strategy → Candidate
 */
public interface ModelRouter {

    /**
     * Route to the best model.
     *
     * @param candidates
     *     available model candidates
     * @param context
     *     routing context
     * @return
     *     the routing decision
     */
    RoutingDecision route(
        List<ModelCandidate> candidates,
        RoutingContext context
    );

    /**
     * Get the routing strategy.
     */
    RoutingStrategy strategy();
}
