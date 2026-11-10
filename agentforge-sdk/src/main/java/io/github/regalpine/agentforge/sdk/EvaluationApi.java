package io.github.regalpine.agentforge.sdk;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Evaluation API facade.
 * AF-ENHANCE-04 §4: provides a convenient
 * entry point for evaluation operations.
 */
public final class EvaluationApi {

    private final Map<String, Object> cases =
        new ConcurrentHashMap<>();
    private final Map<String, Object> results =
        new ConcurrentHashMap<>();

    /**
     * Register an evaluation case.
     */
    public EvaluationApi registerCase(
        String caseId, Object evalCase
    ) {
        cases.put(caseId, evalCase);
        return this;
    }

    /**
     * Get an evaluation case.
     */
    public Optional<Object> getCase(String caseId) {
        return Optional.ofNullable(
            cases.get(caseId)
        );
    }

    /**
     * List all case IDs.
     */
    public List<String> caseIds() {
        return List.copyOf(cases.keySet());
    }

    /**
     * Record a result.
     */
    public EvaluationApi recordResult(
        String runId, Object result
    ) {
        results.put(runId, result);
        return this;
    }

    /**
     * Get a result.
     */
    public Optional<Object> getResult(String runId) {
        return Optional.ofNullable(
            results.get(runId)
        );
    }

    /**
     * Get the number of registered cases.
     */
    public int caseCount() {
        return cases.size();
    }

    /**
     * Get the number of recorded results.
     */
    public int resultCount() {
        return results.size();
    }
}
