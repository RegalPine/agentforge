package io.github.regalpine.agentforge.evaluation.platform;

/**
 * Evaluation run status.
 * AF-ENHANCE-12 §36: Run Lifecycle.
 */
public enum EvaluationRunStatus {

    CREATED,

    VALIDATING,

    READY,

    RUNNING,

    COMPLETED,

    FAILED,

    CANCELLED,

    PARTIAL
}
