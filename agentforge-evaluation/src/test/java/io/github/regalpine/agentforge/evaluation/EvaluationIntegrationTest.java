package io.github.regalpine.agentforge.evaluation;

import io.github.regalpine.agentforge.evaluation.conformance.*;
import io.github.regalpine.agentforge.evaluation.dataset.*;
import io.github.regalpine.agentforge.evaluation.fake.*;
import io.github.regalpine.agentforge.evaluation.harness.*;
import io.github.regalpine.agentforge.evaluation.model.*;
import io.github.regalpine.agentforge.evaluation.replay.*;
import io.github.regalpine.agentforge.evaluation.report.*;
import io.github.regalpine.agentforge.evaluation.runner.*;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * EvaluationIntegrationTest — tests harness, fakes, dataset, replay, report, runner, gate, conformance.
 */
class EvaluationIntegrationTest {

    // --- Fake Harness tests ---

    @Test
    void fakeAgentHarnessShouldReturnConfiguredResponse() {
        var harness = new FakeAgentHarness()
                .withResponse("agent-1", Map.of("answer", "42"));

        var input = new EvaluationInput(Map.of("task", "calculate"), Map.of(), Map.of());
        var result = harness.execute("agent-1", input);

        assertNotNull(result.output());
        assertEquals(1, harness.invocations().size());
        assertEquals("agent-1", harness.invocations().get(0).agentId());
    }

    @Test
    void fakeGraphHarnessShouldReturnConfiguredResponse() {
        var harness = new FakeGraphHarness()
                .withResponse("graph-1", Map.of("nodes", List.of("A", "B", "C")));

        var input = new EvaluationInput(Map.of("task", "execute"), Map.of(), Map.of());
        var result = harness.execute("graph-1", input);

        assertNotNull(result.output());
        assertEquals(1, harness.invocations().size());
    }

    @Test
    void fakeCapabilityHarnessShouldSimulateFailure() {
        var harness = new FakeCapabilityHarness()
                .withFailure("cap-1", "Connection refused");

        var input = new EvaluationInput(Map.of(), Map.of(), Map.of());
        assertThrows(RuntimeException.class, () -> harness.invoke("cap-1", input));
    }

    // --- Deterministic Clock & Random ---

    @Test
    void fixedClockShouldReturnFixedInstant() {
        var now = Instant.parse("2024-01-01T00:00:00Z");
        var clock = new FixedClock(now);

        assertEquals(now, clock.instant());

        clock.advance(1000);
        assertEquals(now.plusMillis(1000), clock.instant());
    }

    @Test
    void deterministicRandomShouldProduceReproducibleSequence() {
        var random1 = new DeterministicRandom(42);
        var random2 = new DeterministicRandom(42);

        for (int i = 0; i < 10; i++) {
            assertEquals(random1.nextInt(), random2.nextInt());
        }
    }

    @Test
    void deterministicRandomShouldReset() {
        var random = new DeterministicRandom(42);
        int first = random.nextInt();

        var reset = random.reset();
        assertEquals(first, reset.nextInt());
    }

    // --- Dataset ---

    @Test
    void evaluationDatasetShouldBeVersioned() {
        var evalCase = new EvaluationCase(
                "case-1", "v1",
                new EvaluationInput(Map.of(), Map.of(), Map.of()),
                ExpectedOutcome.exact("result"),
                EvaluationCriteria.of(Criterion.of("c1", CriterionType.CORRECTNESS, 0.9)));

        var dataset = EvaluationDataset.of("ds-1", "v1.0", List.of(evalCase));

        assertEquals("ds-1", dataset.id());
        assertEquals("v1.0", dataset.version());
        assertEquals(1, dataset.size());
        assertTrue(dataset.findCase("case-1").isPresent());
    }

    // --- Replay ---

    @Test
    void replayRunnerShouldReplayObservations() {
        var data = new ReplayData(
                "exec-1",
                ReplayMode.OBSERVATION_REPLAY,
                Map.of("task", "test"),
                Map.of("state", "initial"),
                List.of(),
                List.of(),
                List.of(new ReplayData.Observation("step-1", "observed")),
                Map.of());

        var runner = new ReplayRunner();
        var result = runner.replay(data);

        assertEquals("exec-1", result.executionId());
        assertTrue(result.isExact());
        assertEquals("observed", result.replayedState().get("obs_step-1"));
    }

    @Test
    void replayRunnerShouldReplayFullData() {
        var data = new ReplayData(
                "exec-2",
                ReplayMode.FULL_REPLAY,
                Map.of("task", "test"),
                Map.of("state", "initial"),
                List.of(new ReplayData.ModelResponse("call-1", "response-1")),
                List.of(new ReplayData.ToolResult("tool-1", "tool-result")),
                List.of(new ReplayData.Observation("step-1", "obs")),
                Map.of());

        var runner = new ReplayRunner();
        var result = runner.replay(data);

        assertEquals("response-1", result.replayedState().get("model_call-1"));
        assertEquals("tool-result", result.replayedState().get("tool_tool-1"));
        assertEquals("obs", result.replayedState().get("obs_step-1"));
    }

    // --- Report ---

    @Test
    void evaluationReportShouldAggregateResults() {
        var results = List.of(
                new EvaluationResult("case-1", EvaluationStatus.PASS, List.of(), new Score(0.95, 0.9), List.of()),
                new EvaluationResult("case-2", EvaluationStatus.PASS, List.of(), new Score(0.85, 0.8), List.of()),
                new EvaluationResult("case-3", EvaluationStatus.FAIL, List.of(), new Score(0.3, 0.8), List.of()));

        var report = ReportFactory.createReport("R-1", "ds-1", "v1", results, Map.of());

        assertEquals(3, report.totalCases());
        assertEquals(2, report.passedCases());
        assertEquals(1, report.failedCases());
        assertFalse(report.allPassed());
    }

    @Test
    void regressionReportShouldDetectRegressions() {
        var baselineResults = List.of(
                new EvaluationResult("case-1", EvaluationStatus.PASS, List.of(), new Score(0.9, 0.8), List.of()),
                new EvaluationResult("case-2", EvaluationStatus.PASS, List.of(), new Score(0.85, 0.8), List.of()));

        var currentResults = List.of(
                new EvaluationResult("case-1", EvaluationStatus.PASS, List.of(), new Score(0.9, 0.8), List.of()),
                new EvaluationResult("case-2", EvaluationStatus.FAIL, List.of(), new Score(0.5, 0.8), List.of()));

        var baseline = ReportFactory.createReport("R-1", "ds-1", "v1", baselineResults, Map.of());
        var current = ReportFactory.createReport("R-2", "ds-1", "v1", currentResults, Map.of());

        var regression = ReportFactory.compareRegression(baseline, current, 0.05);

        assertTrue(regression.hasRegression());
        assertFalse(regression.findings().isEmpty());
    }

    @Test
    void versionComparisonShouldDetectImprovement() {
        var baselineResults = List.of(
                new EvaluationResult("case-1", EvaluationStatus.PASS, List.of(), new Score(0.7, 0.8), List.of()));
        var currentResults = List.of(
                new EvaluationResult("case-1", EvaluationStatus.PASS, List.of(), new Score(0.9, 0.8), List.of()));

        var baseline = ReportFactory.createReport("R-1", "ds-1", "v1", baselineResults, Map.of());
        var current = ReportFactory.createReport("R-2", "ds-1", "v2", currentResults, Map.of());

        var comparison = ReportFactory.compareVersions("agent-1", "v1", "v2", baseline, current);

        assertTrue(comparison.improved());
        assertFalse(comparison.regressed());
    }

    // --- EvaluationRunner ---

    @Test
    void evaluationRunnerShouldRunDatasetAndProduceReport() {
        var harness = new FakeAgentHarness()
                .withResponse("agent-1", "expected-output");

        var evalCase = new EvaluationCase(
                "case-1", "v1",
                new EvaluationInput(Map.of("task", "test"), Map.of(), Map.of()),
                ExpectedOutcome.exact("expected-output"),
                EvaluationCriteria.of(Criterion.of("correctness", CriterionType.CORRECTNESS, 0.9)));

        var dataset = EvaluationDataset.of("ds-1", "v1", List.of(evalCase));

        var runner = new EvaluationRunner(harness);
        var report = runner.run("agent-1", dataset);

        assertEquals(1, report.totalCases());
        assertEquals(1, report.passedCases());
        assertTrue(report.allPassed());
    }

    @Test
    void evaluationRunnerShouldHandleErrors() {
        var harness = new FakeAgentHarness()
                .withResponse("agent-1", input -> { throw new RuntimeException("boom"); });

        var evalCase = new EvaluationCase(
                "case-1", "v1",
                new EvaluationInput(Map.of(), Map.of(), Map.of()),
                ExpectedOutcome.exact("anything"),
                EvaluationCriteria.of(Criterion.of("c1", CriterionType.CORRECTNESS, 0.9)));

        var dataset = EvaluationDataset.of("ds-1", "v1", List.of(evalCase));
        var runner = new EvaluationRunner(harness);
        var report = runner.run("agent-1", dataset);

        assertEquals(1, report.totalCases());
        assertEquals(1, report.errorCases());
    }

    // --- EvaluationGate ---

    @Test
    void strictGateShouldRequireAllPass() {
        var results = List.of(
                new EvaluationResult("case-1", EvaluationStatus.PASS, List.of(), new Score(0.95, 0.9), List.of()),
                new EvaluationResult("case-2", EvaluationStatus.FAIL, List.of(), new Score(0.3, 0.9), List.of()));

        var report = ReportFactory.createReport("R-1", "ds-1", "v1", results, Map.of());
        var gate = EvaluationGate.strict();
        var gateResult = gate.evaluate(report);

        assertFalse(gateResult.passed());
    }

    @Test
    void relaxedGateShouldPassWithGoodAverage() {
        var results = List.of(
                new EvaluationResult("case-1", EvaluationStatus.PASS, List.of(), new Score(0.85, 0.7), List.of()),
                new EvaluationResult("case-2", EvaluationStatus.PASS, List.of(), new Score(0.80, 0.7), List.of()));

        var report = ReportFactory.createReport("R-1", "ds-1", "v1", results, Map.of());
        var gate = EvaluationGate.relaxed();
        var gateResult = gate.evaluate(report);

        assertTrue(gateResult.passed());
    }

    // --- Conformance ---

    @Test
    void conformanceSuiteShouldExecuteAndReport() {
        var suite = ConformanceSuite.of("AF-CONFORMANCE",
                new ConformanceCase("AF-MODEL-001", "AE-01", "Model provider contract",
                        () -> ConformanceResult.pass("AF-MODEL-001")),
                new ConformanceCase("AF-CAP-001", "AE-02", "Capability invocation contract",
                        () -> ConformanceResult.pass("AF-CAP-001")),
                new ConformanceCase("AF-LOOP-001", "AE-05", "Agent loop boundary",
                        () -> ConformanceResult.fail("AF-LOOP-001", "Not implemented")));

        var report = suite.execute(Map.of("java.version", "17"));

        assertEquals(3, report.totalCases());
        assertEquals(2, report.passedCases());
        assertEquals(1, report.failedCases());
        assertFalse(report.isConformant());
    }

    @Test
    void conformanceSuiteShouldHandleExceptions() {
        var suite = ConformanceSuite.of("AF-ERROR",
                new ConformanceCase("ERR-001", "test", "Throws exception",
                        () -> { throw new RuntimeException("boom"); }));

        var report = suite.execute(Map.of());

        assertEquals(1, report.totalCases());
        assertEquals(0, report.passedCases());
        assertEquals(1, report.failedCases());
    }
}
