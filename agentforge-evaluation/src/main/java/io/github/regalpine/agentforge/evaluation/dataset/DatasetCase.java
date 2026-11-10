package io.github.regalpine.agentforge.evaluation.dataset;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Dataset case.
 * AF-ENHANCE-12 §8: DatasetCase.
 */
public record DatasetCase(
    String caseId,
    Object input,
    Object expectedOutput,
    Map<String, Object> context,
    Map<String, Object> metadata
) {

    public DatasetCase {
        Objects.requireNonNull(
            caseId, "caseId"
        );
        Objects.requireNonNull(
            input, "input"
        );

        context = context == null
            ? Map.of() : Map.copyOf(context);
        metadata = metadata == null
            ? Map.of() : Map.copyOf(metadata);
    }

    /**
     * Create a simple case.
     */
    public static DatasetCase of(
        String caseId,
        Object input
    ) {
        return new DatasetCase(
            caseId, input, null,
            Map.of(), Map.of()
        );
    }

    /**
     * Create with expected output.
     */
    public static DatasetCase of(
        String caseId,
        Object input,
        Object expectedOutput
    ) {
        return new DatasetCase(
            caseId, input, expectedOutput,
            Map.of(), Map.of()
        );
    }

    /**
     * Check if expected output exists.
     */
    public boolean hasExpected() {
        return expectedOutput != null;
    }
}
