package io.github.regalpine.agentforge.runtime.structured;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Context shared across pipeline stages.
 * <p>
 * Accumulates errors, warnings, and repair attempts during
 * pipeline execution.
 */
public final class PipelineContext {

    private final List<String> errors = new ArrayList<>();
    private final List<String> warnings = new ArrayList<>();
    private int repairAttempts = 0;
    private final long startTimeMillis;
    private final Map<String, Object> attributes = new HashMap<>();

    public PipelineContext() {
        this.startTimeMillis = System.currentTimeMillis();
    }

    public void addError(String error) {
        errors.add(error);
    }

    public void addWarning(String warning) {
        warnings.add(warning);
    }

    public void incrementRepairAttempts() {
        repairAttempts++;
    }

    public boolean hasErrors() {
        return !errors.isEmpty();
    }

    public List<String> getErrors() {
        return List.copyOf(errors);
    }

    public List<String> getWarnings() {
        return List.copyOf(warnings);
    }

    public int getRepairAttempts() {
        return repairAttempts;
    }

    public long getElapsedMillis() {
        return System.currentTimeMillis() - startTimeMillis;
    }

    public void setAttribute(String key, Object value) {
        attributes.put(key, value);
    }

    @SuppressWarnings("unchecked")
    public <T> T getAttribute(String key) {
        return (T) attributes.get(key);
    }

    public StructuredResult.ProcessingReport toReport() {
        return new StructuredResult.ProcessingReport(
            repairAttempts,
            List.copyOf(errors),
            List.copyOf(warnings),
            getElapsedMillis()
        );
    }
}
