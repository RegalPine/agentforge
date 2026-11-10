package io.github.regalpine.agentforge.evolution;

/**
 * Problem category classification.
 * AF-ENHANCE-15 §24: Problem.
 */
public enum ProblemCategory {

    CORRECTNESS,

    LATENCY,

    COST,

    POLICY_VIOLATION,

    TOOL_FAILURE,

    HALLUCINATION,

    INCOMPLETE_ANSWER,

    REGRESSION
}
