package io.github.regalpine.agentforge.evaluation.platform;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for Evaluation platform types.
 * AF-ENHANCE-12.
 */
class EvaluationPlatformTest {

    // --- EvaluationRunId ---

    @Test
    void runId_basic() {
        var id = new EvaluationRunId("run-1");
        assertEquals("run-1", id.value());
    }

    @Test
    void runId_rejectsNull() {
        assertThrows(
            NullPointerException.class,
            () -> new EvaluationRunId(null)
        );
    }

    // --- EvaluationEnvironment ---

    @Test
    void environment_defaults() {
        var env = EvaluationEnvironment.defaults();

        assertEquals("1.0.0", env.runtimeVersion());
        assertTrue(env.configuration().isEmpty());
    }

    // --- EvaluationEvidence ---

    @Test
    void evidence_basic() {
        var ev = EvaluationEvidence.of(
            "trace", "span-123"
        );

        assertEquals("trace", ev.type());
        assertEquals("span-123", ev.value());
    }

    // --- EvaluationCaseResult ---

    @Test
    void caseResult_basic() {
        var r = new EvaluationCaseResult(
            "case-1", "eval-1",
            EvaluationCaseStatus.PASS,
            0.95,
            List.of(),
            Map.of()
        );

        assertEquals("case-1", r.caseId());
        assertEquals(0.95, r.score());
        assertEquals(
            EvaluationCaseStatus.PASS,
            r.status()
        );
    }

    // --- EvaluationRun ---

    @Test
    void run_basic() {
        var run = new EvaluationRun(
            new EvaluationRunId("run-1"),
            "ds-1", "agent-1", "1.0.0",
            EvaluationRunStatus.COMPLETED,
            null, List.of(), Map.of()
        );

        assertTrue(run.isPassed());
        assertTrue(run.isTerminal());
    }

    @Test
    void run_failed() {
        var run = new EvaluationRun(
            new EvaluationRunId("run-2"),
            "ds-1", "agent-1", "1.0.0",
            EvaluationRunStatus.FAILED,
            null, List.of(), Map.of()
        );

        assertFalse(run.isPassed());
        assertTrue(run.isTerminal());
    }

    @Test
    void run_running() {
        var run = new EvaluationRun(
            new EvaluationRunId("run-3"),
            "ds-1", "agent-1", "1.0.0",
            EvaluationRunStatus.RUNNING,
            null, List.of(), Map.of()
        );

        assertFalse(run.isPassed());
        assertFalse(run.isTerminal());
    }

    // --- EvaluationVariant ---

    @Test
    void variant_basic() {
        var v = EvaluationVariant.of(
            "v1", "agent-a"
        );

        assertEquals("v1", v.variantId());
        assertEquals("agent-a", v.agentReference());
        assertEquals("1.0.0", v.version());
    }

    // --- EvaluationExperiment ---

    @Test
    void experiment_basic() {
        var variants = List.of(
            EvaluationVariant.of("a", "agent-a"),
            EvaluationVariant.of("b", "agent-b")
        );

        var exp = EvaluationExperiment.of(
            "exp-1", "ds-1", variants
        );

        assertEquals("exp-1", exp.experimentId());
        assertEquals(2, exp.variants().size());
        assertEquals(
            EvaluationRunStatus.CREATED,
            exp.status()
        );
    }

    // --- GateResult ---

    @Test
    void gateResult_pass() {
        var g = GateResult.pass();

        assertTrue(g.isPassed());
        assertEquals(GateStatus.PASS, g.status());
    }

    @Test
    void gateResult_fail() {
        var g = GateResult.fail("too low");

        assertFalse(g.isPassed());
        assertEquals("too low", g.message());
    }

    // --- EvaluationRunStatus ---

    @Test
    void runStatus_values() {
        assertEquals(
            8,
            EvaluationRunStatus.values().length
        );
    }

    // --- EvaluationCaseStatus ---

    @Test
    void caseStatus_values() {
        assertEquals(
            6,
            EvaluationCaseStatus.values().length
        );
    }

    // --- GateStatus ---

    @Test
    void gateStatus_values() {
        assertEquals(
            4, GateStatus.values().length
        );
    }

    // --- RegressionType ---

    @Test
    void regressionType_values() {
        assertEquals(
            6,
            RegressionType.values().length
        );
    }

    // --- DatasetId (dataset package) ---

    @Test
    void datasetId_basic() {
        var id = new io.github.regalpine.agentforge.evaluation.dataset.DatasetId(
            "eval", "research", "2.0.0"
        );

        assertEquals("eval", id.namespace());
        assertEquals("research", id.name());
        assertEquals(
            "eval/research/2.0.0",
            id.toString()
        );
    }

    // --- DatasetCase (dataset package) ---

    @Test
    void datasetCase_basic() {
        var c = io.github.regalpine.agentforge.evaluation.dataset.DatasetCase.of(
            "case-1", "input"
        );

        assertEquals("case-1", c.caseId());
        assertFalse(c.hasExpected());
    }

    @Test
    void datasetCase_withExpected() {
        var c = io.github.regalpine.agentforge.evaluation.dataset.DatasetCase.of(
            "case-1", "input", "expected"
        );

        assertTrue(c.hasExpected());
    }
}
