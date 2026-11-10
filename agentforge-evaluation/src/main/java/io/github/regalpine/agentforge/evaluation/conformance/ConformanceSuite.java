package io.github.regalpine.agentforge.evaluation.conformance;

import java.time.Instant;
import java.util.*;

/**
 * ConformanceSuite — AE-12 §59-60.
 *
 * A collection of conformance test cases covering AF-* specifications.
 */
public final class ConformanceSuite {

    private final String suiteId;
    private final List<ConformanceCase> cases;

    public ConformanceSuite(String suiteId, List<ConformanceCase> cases) {
        this.suiteId = Objects.requireNonNull(suiteId, "suiteId");
        this.cases = List.copyOf(Objects.requireNonNull(cases, "cases"));
    }

    public static ConformanceSuite of(String suiteId, ConformanceCase... cases) {
        return new ConformanceSuite(suiteId, List.of(cases));
    }

    public String suiteId() {
        return suiteId;
    }

    public List<ConformanceCase> cases() {
        return cases;
    }

    /**
     * Execute all conformance tests and produce a report.
     */
    public ConformanceReport execute(Map<String, String> environmentFingerprint) {
        List<ConformanceResult> results = new ArrayList<>();
        for (ConformanceCase c : cases) {
            try {
                ConformanceResult result = c.runner().run();
                results.add(result);
            } catch (Exception e) {
                results.add(ConformanceResult.fail(c.id(), "Exception: " + e.getMessage()));
            }
        }
        return new ConformanceReport(
                "CR-" + UUID.randomUUID().toString().substring(0, 8),
                Instant.now(),
                results,
                environmentFingerprint);
    }

    @Override
    public String toString() {
        return "ConformanceSuite{id='" + suiteId + "', cases=" + cases.size() + "}";
    }
}
