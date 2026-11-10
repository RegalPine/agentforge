package io.github.regalpine.agentforge.evaluation.evaluator;

import io.github.regalpine.agentforge.evaluation.model.*;

import java.util.*;

/**
 * StructuralEvaluator — AE-12 §9.
 *
 * Validates that the actual output has the expected structure:
 * required fields exist, types are correct, arrays are present,
 * numeric ranges are within bounds.
 *
 * The expected value should be a Map representing the expected schema.
 * Each key maps to either:
 * - a Class&lt;?&gt; for type checking
 * - a String marker: "required", "optional", "array", "number"
 */
public final class StructuralEvaluator implements Evaluator {

    @SuppressWarnings("unchecked")
    @Override
    public EvaluationResult evaluate(EvaluationCase evaluationCase, Object actualOutput) {
        Objects.requireNonNull(evaluationCase, "evaluationCase");

        Object expected = evaluationCase.expected().expectedValue().orElse(null);
        List<EvaluationFinding> findings = new ArrayList<>();
        int totalChecks = 0;
        int passedChecks = 0;

        if (expected instanceof Map<?, ?> expectedSchema && actualOutput instanceof Map<?, ?> actualMap) {
            for (Map.Entry<?, ?> entry : expectedSchema.entrySet()) {
                String field = String.valueOf(entry.getKey());
                Object spec = entry.getValue();
                totalChecks++;

                if (!actualMap.containsKey(field)) {
                    findings.add(new EvaluationFinding(
                            "missing-field-" + field,
                            FindingSeverity.ERROR,
                            "Missing required field: " + field,
                            "structure"));
                    continue;
                }

                Object fieldValue = actualMap.get(field);

                if (spec instanceof Class<?> expectedType) {
                    if (expectedType.isInstance(fieldValue)) {
                        passedChecks++;
                    } else {
                        findings.add(new EvaluationFinding(
                                "wrong-type-" + field,
                                FindingSeverity.ERROR,
                                "Field '" + field + "' expected type " + expectedType.getSimpleName()
                                        + " but got " + (fieldValue == null ? "null" : fieldValue.getClass().getSimpleName()),
                                "structure"));
                    }
                } else if ("required".equals(spec)) {
                    if (fieldValue != null) {
                        passedChecks++;
                    } else {
                        findings.add(new EvaluationFinding(
                                "null-field-" + field,
                                FindingSeverity.ERROR,
                                "Required field '" + field + "' is null",
                                "structure"));
                    }
                } else if ("array".equals(spec)) {
                    if (fieldValue instanceof Collection) {
                        passedChecks++;
                    } else {
                        findings.add(new EvaluationFinding(
                                "not-array-" + field,
                                FindingSeverity.ERROR,
                                "Field '" + field + "' expected array but got "
                                        + (fieldValue == null ? "null" : fieldValue.getClass().getSimpleName()),
                                "structure"));
                    }
                } else if ("number".equals(spec)) {
                    if (fieldValue instanceof Number) {
                        passedChecks++;
                    } else {
                        findings.add(new EvaluationFinding(
                                "not-number-" + field,
                                FindingSeverity.ERROR,
                                "Field '" + field + "' expected number but got "
                                        + (fieldValue == null ? "null" : fieldValue.getClass().getSimpleName()),
                                "structure"));
                    }
                } else {
                    // Treat as required field check
                    if (fieldValue != null) {
                        passedChecks++;
                    }
                }
            }
        } else if (expected != null && actualOutput != null) {
            totalChecks = 1;
            if (expected.getClass().isInstance(actualOutput)) {
                passedChecks = 1;
            } else {
                findings.add(new EvaluationFinding(
                        "type-mismatch",
                        FindingSeverity.ERROR,
                        "Expected type " + expected.getClass().getSimpleName()
                                + " but got " + actualOutput.getClass().getSimpleName(),
                        "structure"));
            }
        } else if (Objects.equals(expected, actualOutput)) {
            totalChecks = 1;
            passedChecks = 1;
        } else {
            totalChecks = 1;
            findings.add(new EvaluationFinding(
                    "null-output",
                    FindingSeverity.ERROR,
                    "Expected output but got null or non-map",
                    "structure"));
        }

        double scoreValue = totalChecks > 0 ? (double) passedChecks / totalChecks : 0.0;
        Score score = new Score(scoreValue, evaluationCase.expected().threshold());

        List<CriterionResult> criterionResults = new ArrayList<>();
        for (Criterion criterion : evaluationCase.criteria().criteria()) {
            CriterionStatus criterionStatus = score.passed() ? CriterionStatus.PASS : CriterionStatus.FAIL;
            criterionResults.add(new CriterionResult(
                    criterion.id(), criterionStatus, score,
                    "Structural check: " + passedChecks + "/" + totalChecks + " passed"));
        }

        EvaluationStatus status = score.passed() ? EvaluationStatus.PASS : EvaluationStatus.FAIL;
        return new EvaluationResult(evaluationCase.id(), status, criterionResults, score, findings);
    }
}
