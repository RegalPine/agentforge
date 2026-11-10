package io.github.regalpine.agentforge.model;

import java.util.List;

/**
 * Model routing policy —
 * AF-ENHANCE-09 §31.
 *
 * <p>Policy can constrain routing
 * by: tenant, region, data
 * residency, security level,
 * cost budget, latency, capability,
 * provider, availability, quota.
 */
public interface ModelRoutingPolicy {

    /**
     * Evaluate candidates and
     * produce a routing decision.
     *
     * @param context
     *     routing context
     * @param candidates
     *     available candidates
     * @return
     *     the routing decision
     */
    RoutingDecision evaluate(
        RoutingContext context,
        List<ModelCandidate> candidates
    );
}
