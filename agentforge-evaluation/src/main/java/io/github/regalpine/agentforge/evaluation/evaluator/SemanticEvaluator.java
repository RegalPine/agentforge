package io.github.regalpine.agentforge.evaluation.evaluator;

import io.github.regalpine.agentforge.evaluation.model.*;

import java.util.*;

/**
 * SemanticEvaluator — AE-12 §10.
 *
 * Evaluates semantic quality of natural language output:
 * meaning, relevance, completeness, grounding.
 *
 * Uses a configurable SemanticJudge function to score semantic similarity.
 * In production this would delegate to an LLM-as-a-Judge; for testing
 * it can use simple string overlap or a custom function.
 */
public final class SemanticEvaluator implements Evaluator {

    /**
     * A function that scores semantic similarity between actual and reference text.
     */
    @FunctionalInterface
    public interface SemanticJudge {
        /**
         * @param actual   the actual output text
         * @param reference the reference text
         * @return a score in [0, 1]
         */
        double judge(String actual, String reference);
    }

    private final SemanticJudge judge;

    public SemanticEvaluator(SemanticJudge judge) {
        this.judge = Objects.requireNonNull(judge, "judge");
    }

    /**
     * Creates a SemanticEvaluator with a default token-overlap judge.
     */
    public static SemanticEvaluator withTokenOverlap() {
        return new SemanticEvaluator(SemanticEvaluator::tokenOverlap);
    }

    @Override
    public EvaluationResult evaluate(EvaluationCase evaluationCase, Object actualOutput) {
        Objects.requireNonNull(evaluationCase, "evaluationCase");

        Object expected = evaluationCase.expected().expectedValue().orElse(null);
        String actualText = actualOutput != null ? actualOutput.toString() : "";
        String referenceText = expected != null ? expected.toString() : "";

        double similarity = judge.judge(actualText, referenceText);
        Score score = new Score(similarity, evaluationCase.expected().threshold());

        List<CriterionResult> criterionResults = new ArrayList<>();
        for (Criterion criterion : evaluationCase.criteria().criteria()) {
            CriterionStatus status = similarity >= criterion.threshold()
                    ? CriterionStatus.PASS : CriterionStatus.FAIL;
            criterionResults.add(new CriterionResult(
                    criterion.id(), status,
                    new Score(similarity, criterion.threshold()),
                    "Semantic similarity: " + String.format("%.3f", similarity)));
        }

        List<EvaluationFinding> findings = new ArrayList<>();
        if (!score.passed()) {
            findings.add(new EvaluationFinding(
                    "semantic-below-threshold",
                    FindingSeverity.WARNING,
                    "Semantic similarity " + String.format("%.3f", similarity)
                            + " below threshold " + evaluationCase.expected().threshold(),
                    "relevance"));
        }

        EvaluationStatus status = score.passed() ? EvaluationStatus.PASS : EvaluationStatus.FAIL;
        return new EvaluationResult(evaluationCase.id(), status, criterionResults, score, findings);
    }

    /**
     * Simple token-overlap similarity (Jaccard-like).
     */
    public static double tokenOverlap(String a, String b) {
        if (a.isEmpty() && b.isEmpty()) return 1.0;
        if (a.isEmpty() || b.isEmpty()) return 0.0;

        Set<String> tokensA = new HashSet<>(Arrays.asList(a.toLowerCase().split("\\s+")));
        Set<String> tokensB = new HashSet<>(Arrays.asList(b.toLowerCase().split("\\s+")));

        Set<String> intersection = new HashSet<>(tokensA);
        intersection.retainAll(tokensB);

        Set<String> union = new HashSet<>(tokensA);
        union.addAll(tokensB);

        return union.isEmpty() ? 0.0 : (double) intersection.size() / union.size();
    }
}
