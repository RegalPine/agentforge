package io.github.regalpine.agentforge.service;

/**
 * Service health status.
 * AF-ENHANCE-11 §65: Service Health.
 */
public enum ServiceHealth {

    STARTING,

    READY,

    DEGRADED,

    UNAVAILABLE,

    STOPPING,

    STOPPED
}
