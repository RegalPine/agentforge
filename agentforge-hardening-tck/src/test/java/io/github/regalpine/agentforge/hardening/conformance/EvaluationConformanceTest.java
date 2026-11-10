package io.github.regalpine.agentforge.hardening.conformance;

import io.github.regalpine.agentforge.evaluation.evaluator.*;
import io.github.regalpine.agentforge.evaluation.model.*;
import io.github.regalpine.agentforge.evaluation.runner.*;
import io.github.regalpine.agentforge.hardening.*;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * conformance-evaluation: Dataset
 * / Evaluator / Score / Report.
 * AF-RELEASE-02 §4, T-H6.12.
 */
class EvaluationConformanceTest {

    @Test
    void afEval001_scoreCreation() {
        var score = new Score(0.95, 0.8);
        assertEquals(0.95, score.value(), 0.001);
        assertTrue(score.passed());
    }

    @Test
    void afEval002_evaluationStatus() {
        assertNotNull(EvaluationStatus.PASS);
        assertNotNull(EvaluationStatus.FAIL);
        assertNotNull(EvaluationStatus.ERROR);
        assertNotNull(EvaluationStatus.SKIPPED);
    }

    @Test
    void afEval003_evaluatorFactory() {
        var exact = EvaluatorFactory.create(
            OutcomeMode.EXACT
        );
        assertNotNull(exact);
    }

    @Test
    void afEval004_evaluationGate() {
        var gate = EvaluationGate.strict();
        assertNotNull(gate);
        assertEquals(0.9, gate.minimumAverageScore());
    }

    @Test
    void afEval005_criterionTypes() {
        assertNotNull(CriterionType.CORRECTNESS);
        assertNotNull(CriterionType.RELEVANCE);
        assertNotNull(CriterionType.COMPLETENESS);
    }
}
