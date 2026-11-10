package io.github.regalpine.agentforge.runtime.structured;

import java.util.Map;
import java.util.Objects;

/**
 * Result of structured output processing.
 * <p>
 * Contains the parsed and validated structured data along with
 * metadata about the processing pipeline execution.
 */
public record StructuredResult(
    boolean success,
    Map<String, Object> data,
    String rawOutput,
    ProcessingReport report
) {

    public StructuredResult {
        Objects.requireNonNull(report, "report cannot be null");
        data = data == null ? Map.of() : Map.copyOf(data);
    }

    /**
     * Create a successful result.
     *
     * @param data     the structured data
     * @param rawOutput the raw output before processing
     * @param report   the processing report
     * @return a successful StructuredResult
     */
    public static StructuredResult success(Map<String, Object> data, String rawOutput, ProcessingReport report) {
        return new StructuredResult(true, data, rawOutput, report);
    }

    /**
     * Create a failed result.
     *
     * @param rawOutput the raw output before processing
     * @param report    the processing report with errors
     * @return a failed StructuredResult
     */
    public static StructuredResult failure(String rawOutput, ProcessingReport report) {
        return new StructuredResult(false, Map.of(), rawOutput, report);
    }

    /**
     * Processing report with errors and warnings.
     */
    public record ProcessingReport(
        int repairAttempts,
        java.util.List<String> errors,
        java.util.List<String> warnings,
        long processingTimeMillis
    ) {
        public ProcessingReport {
            errors = errors == null ? java.util.List.of() : java.util.List.copyOf(errors);
            warnings = warnings == null ? java.util.List.of() : java.util.List.copyOf(warnings);
        }

        public static ProcessingReport empty() {
            return new ProcessingReport(0, java.util.List.of(), java.util.List.of(), 0);
        }

        public boolean hasErrors() {
            return !errors.isEmpty();
        }

        public boolean hasWarnings() {
            return !warnings.isEmpty();
        }
    }
}
