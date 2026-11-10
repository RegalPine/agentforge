package io.github.regalpine.agentforge.memory;

import java.util.Objects;

/**
 * Token budget for context assembly.
 * AF-ENHANCE-06 §39: Token Budget.
 *
 * <p>Ensures:
 * system + input + context +
 * reserved output <= model window.
 *
 * <p>Complements the existing
 * {@link ContextBudget} which
 * focuses on segment-level
 * allocation.
 */
public record TokenBudget(
    int maxInputTokens,
    int reservedOutputTokens,
    int reservedSystemTokens
) {

    public TokenBudget {
        if (maxInputTokens <= 0) {
            maxInputTokens = 32_000;
        }
        if (reservedOutputTokens < 0) {
            reservedOutputTokens = 4_000;
        }
        if (reservedSystemTokens < 0) {
            reservedSystemTokens = 2_000;
        }
    }

    /**
     * Available tokens for context
     * after reserving system and output.
     */
    public int availableForContext() {
        return Math.max(
            0,
            maxInputTokens
                - reservedSystemTokens
                - reservedOutputTokens
        );
    }

    /**
     * Check if a given token count
     * fits within the available budget.
     */
    public boolean fitsWithin(int tokens) {
        return tokens <= availableForContext();
    }

    /**
     * Create a budget for a 128K model.
     */
    public static TokenBudget large() {
        return new TokenBudget(
            128_000, 16_000, 4_000
        );
    }

    /**
     * Create a budget for a 32K model.
     */
    public static TokenBudget standard() {
        return new TokenBudget(
            32_000, 4_000, 2_000
        );
    }

    /**
     * Small budget for testing.
     */
    public static TokenBudget small() {
        return new TokenBudget(
            4_000, 500, 200
        );
    }

    /**
     * Validate that the total usage
     * does not exceed the budget.
     */
    public BudgetValidation validate(
        int systemTokens,
        int contextTokens,
        int inputTokens
    ) {
        int total =
            systemTokens
                + contextTokens
                + inputTokens
                + reservedOutputTokens;

        boolean within =
            total <= maxInputTokens;

        return new BudgetValidation(
            within,
            maxInputTokens,
            total,
            maxInputTokens - total
        );
    }

    /**
     * Budget validation result.
     */
    public record BudgetValidation(
        boolean withinBudget,
        int maxTokens,
        int usedTokens,
        int remainingTokens
    ) {}
}
