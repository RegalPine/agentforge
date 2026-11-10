package io.github.regalpine.agentforge.evaluation.runner;

import io.github.regalpine.agentforge.evaluation.dataset.EvaluationDataset;
import io.github.regalpine.agentforge.evaluation.evaluator.Evaluator;
import io.github.regalpine.agentforge.evaluation.evaluator.EvaluatorFactory;
import io.github.regalpine.agentforge.evaluation.harness.AgentTestHarness;
import io.github.regalpine.agentforge.evaluation.harness.ExecutionResult;
import io.github.regalpine.agentforge.evaluation.model.*;
import io.github.regalpine.agentforge.evaluation.report.EvaluationReport;
import io.github.regalpine.agentforge.evaluation.report.ReportFactory;

import java.util.*;

/**
 * EvaluationRunner — AE-12 §15, §51.
 *
 * Orchestrates evaluation execution: dataset → harness → evaluator → report.
 */
public final class EvaluationRunner {

    private final AgentTestHarness harness;
    private final Map<String, String> environmentFingerprint;

    public EvaluationRunner(AgentTestHarness harness) {
        this(harness, defaultFingerprint());
    }

    public EvaluationRunner(AgentTestHarness harness, Map<String, String> environmentFingerprint) {
        this.harness = Objects.requireNonNull(harness, "harness");
        this.environmentFingerprint = Map.copyOf(Objects.requireNonNull(environmentFingerprint, "environmentFingerprint"));
    }

    /**
     * Run all cases in a dataset against an agent.
     */
    public EvaluationReport run(String agentId, EvaluationDataset dataset) {
        Objects.requireNonNull(agentId, "agentId");
        Objects.requireNonNull(dataset, "dataset");

        List<EvaluationResult> results = new ArrayList<>();

        for (EvaluationCase evaluationCase : dataset.cases()) {
            try {
                // Execute via harness
                ExecutionResult executionResult = harness.execute(agentId, evaluationCase.input());

                // Create evaluator based on outcome mode
                Evaluator evaluator = EvaluatorFactory.create(evaluationCase.expected().mode());

                // Evaluate
                EvaluationResult evalResult = evaluator.evaluate(evaluationCase, executionResult.output());
                results.add(evalResult);
            } catch (Exception e) {
                // Record error
                results.add(new EvaluationResult(
                        evaluationCase.id(),
                        EvaluationStatus.ERROR,
                        List.of(),
                        new Score(0.0, evaluationCase.expected().threshold()),
                        List.of(new EvaluationFinding(
                                "execution-error",
                                FindingSeverity.CRITICAL,
                                "Execution error: " + e.getMessage(),
                                "error"))));
            }
        }

        return ReportFactory.createReport(
                "ER-" + UUID.randomUUID().toString().substring(0, 8),
                dataset.id(),
                dataset.version(),
                results,
                environmentFingerprint);
    }

    private static Map<String, String> defaultFingerprint() {
        return Map.of(
                "java.version", System.getProperty("java.version", "unknown"),
                "os.name", System.getProperty("os.name", "unknown"),
                "os.arch", System.getProperty("os.arch", "unknown"));
    }
}
