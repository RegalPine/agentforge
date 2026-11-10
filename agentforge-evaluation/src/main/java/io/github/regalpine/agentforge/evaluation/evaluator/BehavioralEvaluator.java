package io.github.regalpine.agentforge.evaluation.evaluator;

import io.github.regalpine.agentforge.evaluation.model.*;

import java.util.*;

/**
 * BehavioralEvaluator — AE-12 §11.
 *
 * Evaluates what the Agent actually did: which tools were called,
 * whether unauthorized tools were rejected, whether necessary retrieval
 * was performed, whether delegation was correct, whether max iterations
 * were respected.
 *
 * The expected behavior is a list of expected actions (e.g., tool calls).
 * The actual behavior is compared against these expectations.
 */
public final class BehavioralEvaluator implements Evaluator {

    @SuppressWarnings("unchecked")
    @Override
    public EvaluationResult evaluate(EvaluationCase evaluationCase, Object actualOutput) {
        Objects.requireNonNull(evaluationCase, "evaluationCase");

        Object expected = evaluationCase.expected().expectedValue().orElse(null);
        List<EvaluationFinding> findings = new ArrayList<>();

        int totalChecks = 0;
        int passedChecks = 0;

        if (expected instanceof List<?> expectedActions && actualOutput instanceof List<?> actualActions) {
            for (Object expectedAction : expectedActions) {
                totalChecks++;
                boolean found = actualActions.stream()
                        .anyMatch(actual -> matchesAction(expectedAction, actual));
                if (found) {
                    passedChecks++;
                } else {
                    findings.add(new EvaluationFinding(
                            "missing-action-" + expectedAction,
                            FindingSeverity.ERROR,
                            "Expected action not found: " + expectedAction,
                            "behavior"));
                }
            }

            // Check for unexpected actions
            for (Object actualAction : actualActions) {
                boolean unexpected = expectedActions.stream()
                        .noneMatch(exp -> matchesAction(exp, actualAction));
                if (unexpected) {
                    findings.add(new EvaluationFinding(
                            "unexpected-action-" + actualAction,
                            FindingSeverity.WARNING,
                            "Unexpected action found: " + actualAction,
                            "behavior"));
                }
            }
        } else if (expected instanceof Map<?, ?> expectedBehavior && actualOutput instanceof Map<?, ?> actualBehavior) {
            // Compare behavior as key-value pairs (e.g., tool call counts)
            for (Map.Entry<?, ?> entry : expectedBehavior.entrySet()) {
                totalChecks++;
                String key = String.valueOf(entry.getKey());
                Object expectedValue = entry.getValue();
                Object actualValue = actualBehavior.get(key);
                if (Objects.equals(expectedValue, actualValue)) {
                    passedChecks++;
                } else {
                    findings.add(new EvaluationFinding(
                            "behavior-mismatch-" + key,
                            FindingSeverity.ERROR,
                            "Behavior '" + key + "': expected " + expectedValue + " but got " + actualValue,
                            "behavior"));
                }
            }
        } else {
            totalChecks = 1;
            if (Objects.equals(expected, actualOutput)) {
                passedChecks = 1;
            } else {
                findings.add(new EvaluationFinding(
                        "behavior-mismatch",
                        FindingSeverity.ERROR,
                        "Expected behavior " + expected + " but got " + actualOutput,
                        "behavior"));
            }
        }

        double scoreValue = totalChecks > 0 ? (double) passedChecks / totalChecks : 0.0;
        Score score = new Score(scoreValue, evaluationCase.expected().threshold());

        List<CriterionResult> criterionResults = new ArrayList<>();
        for (Criterion criterion : evaluationCase.criteria().criteria()) {
            CriterionStatus status = score.passed() ? CriterionStatus.PASS : CriterionStatus.FAIL;
            criterionResults.add(new CriterionResult(
                    criterion.id(), status, score,
                    "Behavioral check: " + passedChecks + "/" + totalChecks + " passed"));
        }

        EvaluationStatus status = score.passed() ? EvaluationStatus.PASS : EvaluationStatus.FAIL;
        return new EvaluationResult(evaluationCase.id(), status, criterionResults, score, findings);
    }

    private boolean matchesAction(Object expected, Object actual) {
        if (Objects.equals(expected, actual)) return true;
        if (expected instanceof Map<?, ?> expectedMap && actual instanceof Map<?, ?> actualMap) {
            for (Map.Entry<?, ?> entry : expectedMap.entrySet()) {
                if (!Objects.equals(entry.getValue(), actualMap.get(entry.getKey()))) {
                    return false;
                }
            }
            return true;
        }
        return false;
    }
}
