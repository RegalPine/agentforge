package io.github.regalpine.agentforge.evolution;

/**
 * Evidence type classification.
 * AF-ENHANCE-15 §23: Evidence.
 */
public enum EvidenceType {

    EVALUATION_RUN,

    EVALUATION_CASE,

    EXECUTION,

    TRACE,

    REGRESSION,

    HUMAN_REVIEW,

    PRODUCTION_METRIC,

    FAILURE_ANALYSIS
}
