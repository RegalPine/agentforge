package io.github.regalpine.agentforge.evaluation.runner;

import io.github.regalpine.agentforge.evaluation.report.EvaluationReport;
import io.github.regalpine.agentforge.evaluation.report.RegressionReport;
import io.github.regalpine.agentforge.evaluation.report.ReportFactory;

/**
 * EvaluationGate — AE-12 §55.
 *
 * CI/CD gate that determines whether a build should pass based on evaluation results.
 */
public final class EvaluationGate {

    private final double minimumAverageScore;
    private final double regressionThreshold;
    private final boolean requireAllPass;

    public EvaluationGate(double minimumAverageScore, double regressionThreshold, boolean requireAllPass) {
        this.minimumAverageScore = minimumAverageScore;
        this.regressionThreshold = regressionThreshold;
        this.requireAllPass = requireAllPass;
    }

    /**
     * Creates a strict gate: all cases must pass, minimum score 0.9.
     */
    public static EvaluationGate strict() {
        return new EvaluationGate(0.9, 0.01, true);
    }

    /**
     * Creates a relaxed gate: average score >= 0.7, regressions allowed up to 0.05.
     */
    public static EvaluationGate relaxed() {
        return new EvaluationGate(0.7, 0.05, false);
    }

    /**
     * Evaluate a single report against this gate.
     */
    public GateResult evaluate(EvaluationReport report) {
        boolean passed = true;
        String reason = "";

        if (report.averageScore() < minimumAverageScore) {
            passed = false;
            reason = "Average score " + String.format("%.3f", report.averageScore())
                    + " below minimum " + minimumAverageScore;
        }

        if (requireAllPass && !report.allPassed()) {
            passed = false;
            reason = reason.isEmpty()
                    ? "Not all cases passed: " + report.failedCases() + " failures"
                    : reason + "; " + report.failedCases() + " failures";
        }

        return new GateResult(passed, reason, report);
    }

    /**
     * Evaluate regression between baseline and current reports.
     */
    public GateResult evaluateRegression(EvaluationReport baseline, EvaluationReport current) {
        RegressionReport regression = ReportFactory.compareRegression(baseline, current, regressionThreshold);

        if (regression.hasRegression()) {
            return new GateResult(false,
                    "Regression detected: " + regression.findings().size() + " findings",
                    current);
        }

        return evaluate(current);
    }

    public double minimumAverageScore() {
        return minimumAverageScore;
    }

    public double regressionThreshold() {
        return regressionThreshold;
    }

    public boolean requireAllPass() {
        return requireAllPass;
    }

    public record GateResult(boolean passed, String reason, EvaluationReport report) {
        @Override
        public String toString() {
            return "GateResult{passed=" + passed + ", reason='" + reason + "'}";
        }
    }
}
