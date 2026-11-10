package io.github.regalpine.agentforge.controlplane;

/**
 * Health check status.
 * AF-ENHANCE-14 §31: Health Check.
 */
public enum HealthStatus {

    HEALTHY,

    UNHEALTHY,

    DEGRADED,

    UNKNOWN
}
