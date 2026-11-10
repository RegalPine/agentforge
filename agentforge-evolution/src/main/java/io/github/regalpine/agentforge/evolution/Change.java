package io.github.regalpine.agentforge.evolution;

/**
 * A single change in a candidate.
 * AF-ENHANCE-15 §16: Change.
 *
 * <p>Each change must specify target,
 * operation, and reason.
 */
public record Change(
    String target,
    ChangeOperation operation,
    String before,
    String after,
    String reason
) {

    public Change {
        if (target == null
            || target.isBlank()) {
            throw new IllegalArgumentException(
                "target required");
        }
        if (operation == null) {
            throw new IllegalArgumentException(
                "operation required");
        }
    }

    /**
     * Create a replace change.
     */
    public static Change replace(
        String target,
        String before,
        String after,
        String reason
    ) {
        return new Change(
            target,
            ChangeOperation.REPLACE,
            before, after, reason
        );
    }
}
