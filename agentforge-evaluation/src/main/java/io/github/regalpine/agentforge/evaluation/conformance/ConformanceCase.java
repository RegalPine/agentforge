package io.github.regalpine.agentforge.evaluation.conformance;

import java.util.List;

/**
 * ConformanceCase — AE-12 §59-60.
 *
 * A single conformance test case, e.g. AF-MODEL-001, AF-CAP-001.
 */
public final class ConformanceCase {

    private final String id;
    private final String specRef;
    private final String description;
    private final ConformanceRunner runner;

    public ConformanceCase(String id, String specRef, String description, ConformanceRunner runner) {
        this.id = id;
        this.specRef = specRef;
        this.description = description;
        this.runner = runner;
    }

    public String id() {
        return id;
    }

    public String specRef() {
        return specRef;
    }

    public String description() {
        return description;
    }

    public ConformanceRunner runner() {
        return runner;
    }

    @FunctionalInterface
    public interface ConformanceRunner {
        ConformanceResult run();
    }
}
