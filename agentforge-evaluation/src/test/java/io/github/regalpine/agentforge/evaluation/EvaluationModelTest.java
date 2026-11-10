package io.github.regalpine.agentforge.evaluation;

import io.github.regalpine.agentforge.evaluation.evaluator.*;
import io.github.regalpine.agentforge.evaluation.model.*;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * EvaluationModelTest — tests core evaluation models and all 5 evaluator types.
 */
class EvaluationModelTest {

    // --- Model tests ---

    @Test
    void evaluationInputShouldStoreTaskContextParameters() {
        var input = new EvaluationInput(
                Map.of("task", "analyze"),
                Map.of("context", "data"),
                Map.of("param1", "value1"));

        assertEquals("analyze", input.task().get("task"));
        assertEquals("data", input.context().get("context"));
        assertTrue(input.getParameter("param1").isPresent());
        assertEquals("value1", input.getParameter("param1").get());
    }

    @Test
    void scoreShouldValidateRange() {
        var score = new Score(0.85, 0.80);
        assertEquals(0.85, score.value());
        assertEquals(0.80, score.threshold());
        assertTrue(score.passed());

        var failingScore = new Score(0.5, 0.80);
        assertFalse(failingScore.passed());
    }

    @Test
    void scoreShouldRejectInvalidValues() {
        assertThrows(IllegalArgumentException.class, () -> new Score(-0.1, 0.0));
        assertThrows(IllegalArgumentException.class, () -> new Score(1.1, 0.0));
    }

    @Test
    void criterionShouldValidateWeightAndThreshold() {
        var criterion = Criterion.of("correctness", CriterionType.CORRECTNESS, 0.9);
        assertEquals("correctness", criterion.id());
        assertEquals(CriterionType.CORRECTNESS, criterion.type());
        assertEquals(1.0, criterion.weight());
        assertEquals(0.9, criterion.threshold());
    }

    @Test
    void criterionShouldRejectInvalidWeight() {
        assertThrows(IllegalArgumentException.class,
                () -> new Criterion("id", "name", "desc", CriterionType.CORRECTNESS, 1.5, 0.9));
    }

    @Test
    void evaluationCaseShouldBeIdentifiable() {
        var input = new EvaluationInput(Map.of(), Map.of(), Map.of());
        var expected = ExpectedOutcome.exact("result");
        var criteria = EvaluationCriteria.of(Criterion.of("c1", CriterionType.CORRECTNESS, 0.9));
        var evalCase = new EvaluationCase("case-1", "v1.0", input, expected, criteria);

        assertEquals("case-1", evalCase.id());
        assertEquals("v1.0", evalCase.version());
    }

    // --- Exact Evaluator ---

    @Test
    void exactEvaluatorShouldPassOnMatch() {
        var input = new EvaluationInput(Map.of(), Map.of(), Map.of());
        var expected = ExpectedOutcome.exact("hello");
        var criteria = EvaluationCriteria.of(Criterion.of("correctness", CriterionType.CORRECTNESS, 0.9));
        var evalCase = new EvaluationCase("exact-1", "v1", input, expected, criteria);

        var evaluator = new ExactEvaluator();
        var result = evaluator.evaluate(evalCase, "hello");

        assertEquals(EvaluationStatus.PASS, result.status());
        assertEquals(1.0, result.score().value());
        assertTrue(result.findings().isEmpty());
    }

    @Test
    void exactEvaluatorShouldFailOnMismatch() {
        var input = new EvaluationInput(Map.of(), Map.of(), Map.of());
        var expected = ExpectedOutcome.exact("hello");
        var criteria = EvaluationCriteria.of(Criterion.of("correctness", CriterionType.CORRECTNESS, 0.9));
        var evalCase = new EvaluationCase("exact-2", "v1", input, expected, criteria);

        var evaluator = new ExactEvaluator();
        var result = evaluator.evaluate(evalCase, "world");

        assertEquals(EvaluationStatus.FAIL, result.status());
        assertEquals(0.0, result.score().value());
        assertFalse(result.findings().isEmpty());
    }

    // --- Structural Evaluator ---

    @Test
    void structuralEvaluatorShouldCheckFieldPresence() {
        var input = new EvaluationInput(Map.of(), Map.of(), Map.of());
        var expectedSchema = Map.of("name", "required", "score", "number");
        var expected = ExpectedOutcome.structural(expectedSchema, 0.8);
        var criteria = EvaluationCriteria.of(Criterion.of("structure", CriterionType.STRUCTURE, 0.8));
        var evalCase = new EvaluationCase("struct-1", "v1", input, expected, criteria);

        var evaluator = new StructuralEvaluator();
        var actual = Map.of("name", "Alice", "score", 85);
        var result = evaluator.evaluate(evalCase, actual);

        assertEquals(EvaluationStatus.PASS, result.status());
    }

    @Test
    void structuralEvaluatorShouldDetectMissingFields() {
        var input = new EvaluationInput(Map.of(), Map.of(), Map.of());
        var expectedSchema = Map.of("name", "required", "email", "required");
        var expected = ExpectedOutcome.structural(expectedSchema, 0.8);
        var criteria = EvaluationCriteria.of(Criterion.of("structure", CriterionType.STRUCTURE, 0.8));
        var evalCase = new EvaluationCase("struct-2", "v1", input, expected, criteria);

        var evaluator = new StructuralEvaluator();
        var actual = Map.of("name", "Alice"); // missing email
        var result = evaluator.evaluate(evalCase, actual);

        assertEquals(EvaluationStatus.FAIL, result.status());
        assertFalse(result.findings().isEmpty());
    }

    // --- Semantic Evaluator ---

    @Test
    void semanticEvaluatorShouldScoreTokenOverlap() {
        var input = new EvaluationInput(Map.of(), Map.of(), Map.of());
        var expected = ExpectedOutcome.semantic("the quick brown fox", 0.5);
        var criteria = EvaluationCriteria.of(Criterion.of("relevance", CriterionType.RELEVANCE, 0.5));
        var evalCase = new EvaluationCase("sem-1", "v1", input, expected, criteria);

        var evaluator = SemanticEvaluator.withTokenOverlap();
        var result = evaluator.evaluate(evalCase, "the quick brown fox jumps");

        assertTrue(result.score().value() > 0.5);
    }

    @Test
    void semanticEvaluatorShouldHandleEmptyStrings() {
        assertEquals(1.0, SemanticEvaluator.tokenOverlap("", ""));
        assertEquals(0.0, SemanticEvaluator.tokenOverlap("hello", ""));
        assertEquals(0.0, SemanticEvaluator.tokenOverlap("", "hello"));
    }

    // --- Behavioral Evaluator ---

    @Test
    void behavioralEvaluatorShouldCheckActions() {
        var input = new EvaluationInput(Map.of(), Map.of(), Map.of());
        var expectedActions = List.of("search", "analyze", "finish");
        var expected = ExpectedOutcome.behavioral(expectedActions);
        var criteria = EvaluationCriteria.of(Criterion.of("tool_usage", CriterionType.TOOL_USAGE, 0.9));
        var evalCase = new EvaluationCase("beh-1", "v1", input, expected, criteria);

        var evaluator = new BehavioralEvaluator();
        var actualActions = List.of("search", "analyze", "finish");
        var result = evaluator.evaluate(evalCase, actualActions);

        assertEquals(EvaluationStatus.PASS, result.status());
    }

    @Test
    void behavioralEvaluatorShouldDetectMissingActions() {
        var input = new EvaluationInput(Map.of(), Map.of(), Map.of());
        var expectedActions = List.of("search", "analyze", "finish");
        var expected = ExpectedOutcome.behavioral(expectedActions);
        var criteria = EvaluationCriteria.of(Criterion.of("tool_usage", CriterionType.TOOL_USAGE, 0.9));
        var evalCase = new EvaluationCase("beh-2", "v1", input, expected, criteria);

        var evaluator = new BehavioralEvaluator();
        var actualActions = List.of("search", "finish"); // missing analyze
        var result = evaluator.evaluate(evalCase, actualActions);

        assertEquals(EvaluationStatus.FAIL, result.status());
    }

    // --- Policy Evaluator ---

    @Test
    void policyEvaluatorShouldPassOnMatch() {
        var input = new EvaluationInput(Map.of(), Map.of(), Map.of());
        var expected = ExpectedOutcome.policy(PolicyEffect.DENY);
        var criteria = EvaluationCriteria.of(Criterion.of("policy", CriterionType.POLICY_COMPLIANCE, 1.0));
        var evalCase = new EvaluationCase("pol-1", "v1", input, expected, criteria);

        var evaluator = new PolicyEvaluator();
        var result = evaluator.evaluate(evalCase, PolicyEffect.DENY);

        assertEquals(EvaluationStatus.PASS, result.status());
    }

    @Test
    void policyEvaluatorShouldFailOnMismatch() {
        var input = new EvaluationInput(Map.of(), Map.of(), Map.of());
        var expected = ExpectedOutcome.policy(PolicyEffect.ALLOW);
        var criteria = EvaluationCriteria.of(Criterion.of("policy", CriterionType.POLICY_COMPLIANCE, 1.0));
        var evalCase = new EvaluationCase("pol-2", "v1", input, expected, criteria);

        var evaluator = new PolicyEvaluator();
        var result = evaluator.evaluate(evalCase, PolicyEffect.DENY);

        assertEquals(EvaluationStatus.FAIL, result.status());
    }

    // --- EvaluatorFactory ---

    @Test
    void evaluatorFactoryShouldCreateCorrectEvaluators() {
        assertInstanceOf(ExactEvaluator.class, EvaluatorFactory.create(OutcomeMode.EXACT));
        assertInstanceOf(StructuralEvaluator.class, EvaluatorFactory.create(OutcomeMode.STRUCTURAL));
        assertInstanceOf(SemanticEvaluator.class, EvaluatorFactory.create(OutcomeMode.SEMANTIC));
        assertInstanceOf(BehavioralEvaluator.class, EvaluatorFactory.create(OutcomeMode.BEHAVIORAL));
        assertInstanceOf(PolicyEvaluator.class, EvaluatorFactory.create(OutcomeMode.POLICY));
    }
}
