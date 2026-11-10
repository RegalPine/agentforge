package io.github.regalpine.agentforge.controlplane;

/**
 * Load balancing strategies.
 * AF-ENHANCE-14 §29: Load Balancing.
 */
public enum LoadBalancingStrategy {

    ROUND_ROBIN,

    LEAST_LOAD,

    WEIGHTED,

    CAPABILITY_AWARE
}
