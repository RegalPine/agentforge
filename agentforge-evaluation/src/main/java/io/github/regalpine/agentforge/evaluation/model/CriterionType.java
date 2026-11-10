package io.github.regalpine.agentforge.evaluation.model;

/**
 * CriterionType — AE-12 §14.
 *
 * MVP criterion types.
 */
public enum CriterionType {
    CORRECTNESS,
    RELEVANCE,
    COMPLETENESS,
    GROUNDEDNESS,
    STRUCTURE,
    LATENCY,
    COST,
    POLICY_COMPLIANCE,
    TOOL_USAGE,
    STATE_TRANSITION,
    SIDE_EFFECT,
    PERFORMANCE
}
