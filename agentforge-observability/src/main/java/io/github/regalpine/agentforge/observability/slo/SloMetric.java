package io.github.regalpine.agentforge.observability.slo;

/**
 * SLO metric type.
 */
public enum SloMetric {
    AVAILABILITY,
    LATENCY_P50,
    LATENCY_P95,
    LATENCY_P99,
    ERROR_RATE,
    TOKEN_THROUGHPUT,
    RECOVERY_TIME
}
