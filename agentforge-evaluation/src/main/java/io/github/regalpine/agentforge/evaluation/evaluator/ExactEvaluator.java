package io.github.regalpine.agentforge.evaluation.evaluator;

import io.github.regalpine.agentforge.evaluation.model.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * ExactEvaluator — AE-12 §8.
 *
 * Compares actual output with expected output using exact equality.
 * Suitable for deterministic components: JSON, Schema, State Transition,
 * Tool Arguments, Policy Decision.
 */
public final class ExactEvaluator implements Evaluator {

    @Override
    public EvaluationResult evaluate(EvaluationCase evaluationCase, Object actualOutput) {
        Objects.requireNonNull(evaluationCase, "evaluationCase");

        Object expected = evaluationCase.expected().expectedValue().orElse(null);
        boolean match = Objects.equals(expected, actualOutput);

        List<CriterionResult> criterionResults = new ArrayList<>();
        for (Criterion criterion : evaluationCase.criteria().criteria()) {
            double scoreValue = match ? 1.0 : 0.0;
            Score score = new Score(scoreValue, criterion.threshold());
            CriterionStatus status = score.passed() ? CriterionStatus.PASS : CriterionStatus.FAIL;
            criterionResults.add(new CriterionResult(
                    criterion.id(), status, score,
                    match ? "Exact match" : "Expected " + expected + " but got " + actualOutput));
        }

        double overallScore = match ? 1.0 : 0.0;
        Score score = new Score(overallScore, evaluationCase.expected().threshold());
        EvaluationStatus status = match ? EvaluationStatus.PASS : EvaluationStatus.FAIL;

        List<EvaluationFinding> findings = new ArrayList<>();
        if (!match) {
            findings.add(new EvaluationFinding(
                    "exact-mismatch",
                    FindingSeverity.ERROR,
                    "Expected " + expected + " but got " + actualOutput,
                    "correctness"));
        }

        return new EvaluationResult(evaluationCase.id(), status, criterionResults, score, findings);
    }
}
