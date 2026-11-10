package io.github.regalpine.agentforge.observability.alert;

/**
 * Alert type.
 * AF-ENHANCE-06 §11.
 */
public enum AlertType {
    SLO_VIOLATION,
    ERROR_RATE_SPIKE,
    LATENCY_DEGRADATION,
    COST_THRESHOLD,
    PROVIDER_UNAVAILABLE
}
