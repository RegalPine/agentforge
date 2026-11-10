package io.github.regalpine.agentforge.evaluation.evaluator;

import io.github.regalpine.agentforge.evaluation.model.EvaluationCase;
import io.github.regalpine.agentforge.evaluation.model.EvaluationResult;

/**
 * Evaluator — AE-12 §4.
 *
 * Evaluates an actual output against an EvaluationCase's expected outcome.
 */
public interface Evaluator {

    /**
     * Evaluate the actual output produced by executing the given case.
     *
     * @param evaluationCase the case being evaluated
     * @param actualOutput   the actual output from execution
     * @return the evaluation result with scores and findings
     */
    EvaluationResult evaluate(EvaluationCase evaluationCase, Object actualOutput);
}
