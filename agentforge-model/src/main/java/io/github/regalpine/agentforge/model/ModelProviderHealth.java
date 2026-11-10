package io.github.regalpine.agentforge.model;

/**
 * Health status of a model provider.
 */
public enum ModelProviderHealth {
    /**
     * Provider is fully operational.
     */
    AVAILABLE,

    /**
     * Provider is operational but with degraded performance.
     */
    DEGRADED,

    /**
     * Provider is not available.
     */
    UNAVAILABLE,

    /**
     * Health status is unknown.
     */
    UNKNOWN
}
