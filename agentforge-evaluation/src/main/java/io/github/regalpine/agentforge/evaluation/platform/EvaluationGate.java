package io.github.regalpine.agentforge.evaluation.platform;

/**
 * Evaluation gate interface.
 * AF-ENHANCE-12 §60: EvaluationGate.
 */
public interface EvaluationGate {

    /**
     * Evaluate a run and produce
     * a gate result.
     */
    GateResult evaluate(
        EvaluationRun run
    );
}
