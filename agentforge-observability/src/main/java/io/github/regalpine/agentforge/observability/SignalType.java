package io.github.regalpine.agentforge.observability;

/**
 * Signal type classification.
 * AF-ENHANCE-06 §3: four signal types.
 */
public enum SignalType {

    /**
     * Discrete occurrence.
     */
    EVENT,

    /**
     * Distributed trace span.
     */
    TRACE,

    /**
     * Numeric measurement.
     */
    METRIC,

    /**
     * Security / compliance record.
     */
    AUDIT
}
