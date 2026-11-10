package io.github.regalpine.agentforge.evaluation.harness;

import io.github.regalpine.agentforge.evaluation.model.EvaluationInput;

import java.util.Map;

/**
 * ExecutionResult — the result of a test harness execution.
 */
public final class ExecutionResult {

    private final Object output;
    private final Map<String, Object> metadata;
    private final long durationMs;

    public ExecutionResult(Object output, Map<String, Object> metadata, long durationMs) {
        this.output = output;
        this.metadata = Map.copyOf(metadata);
        this.durationMs = durationMs;
    }

    public static ExecutionResult of(Object output) {
        return new ExecutionResult(output, Map.of(), 0L);
    }

    public static ExecutionResult of(Object output, long durationMs) {
        return new ExecutionResult(output, Map.of(), durationMs);
    }

    public Object output() {
        return output;
    }

    public Map<String, Object> metadata() {
        return metadata;
    }

    public long durationMs() {
        return durationMs;
    }
}
