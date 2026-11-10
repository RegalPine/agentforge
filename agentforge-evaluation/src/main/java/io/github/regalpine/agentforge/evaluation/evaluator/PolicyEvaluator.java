package io.github.regalpine.agentforge.evaluation.evaluator;

import io.github.regalpine.agentforge.evaluation.model.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * PolicyEvaluator — AE-12 §12.
 *
 * Validates that the actual policy decision matches the expected policy effect:
 * ALLOW, DENY, REQUIRE_HUMAN.
 */
public final class PolicyEvaluator implements Evaluator {

    @Override
    public EvaluationResult evaluate(EvaluationCase evaluationCase, Object actualOutput) {
        Objects.requireNonNull(evaluationCase, "evaluationCase");

        Object expected = evaluationCase.expected().expectedValue().orElse(null);
        List<EvaluationFinding> findings = new ArrayList<>();

        boolean match;
        if (expected instanceof PolicyEffect expectedEffect) {
            if (actualOutput instanceof PolicyEffect actualEffect) {
                match = expectedEffect == actualEffect;
            } else {
                match = false;
            }
        } else {
            match = Objects.equals(expected, actualOutput);
        }

        double scoreValue = match ? 1.0 : 0.0;
        Score score = new Score(scoreValue, evaluationCase.expected().threshold());

        List<CriterionResult> criterionResults = new ArrayList<>();
        for (Criterion criterion : evaluationCase.criteria().criteria()) {
            CriterionStatus status = match ? CriterionStatus.PASS : CriterionStatus.FAIL;
            criterionResults.add(new CriterionResult(
                    criterion.id(), status, score,
                    match ? "Policy decision matches" : "Policy decision mismatch"));
        }

        if (!match) {
            findings.add(new EvaluationFinding(
                    "policy-mismatch",
                    FindingSeverity.CRITICAL,
                    "Expected policy effect " + expected + " but got " + actualOutput,
                    "policy_compliance"));
        }

        EvaluationStatus status = match ? EvaluationStatus.PASS : EvaluationStatus.FAIL;
        return new EvaluationResult(evaluationCase.id(), status, criterionResults, score, findings);
    }
}
