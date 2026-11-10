package io.github.regalpine.agentforge.capability;

/**
 * Capability health status.
 * AE-02 §23: Capability Health.
 *
 * <p>Providers report health so the scheduler
 * can avoid unavailable capabilities.
 */
public enum CapabilityHealth {

    AVAILABLE,

    DEGRADED,

    UNAVAILABLE,

    UNKNOWN
}
