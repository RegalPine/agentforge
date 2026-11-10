package io.github.regalpine.agentforge.evolution;

/**
 * Validation result.
 * AF-ENHANCE-15 §32: Candidate
 * Validation.
 */
public record ValidationResult(
    String gateName,
    boolean passed,
    String message,
    String evidence
) {

    public ValidationResult {
        if (gateName == null
            || gateName.isBlank()) {
            throw new IllegalArgumentException(
                "gateName required");
        }
    }

    /**
     * Create a pass result.
     */
    public static ValidationResult pass(
        String gateName
    ) {
        return new ValidationResult(
            gateName, true,
            "passed", null);
    }

    /**
     * Create a fail result.
     */
    public static ValidationResult fail(
        String gateName,
        String message
    ) {
        return new ValidationResult(
            gateName, false,
            message, null);
    }
}
