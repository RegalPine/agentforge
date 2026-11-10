package io.github.regalpine.agentforge.memory;

/**
 * Context budget.
 * AE-08 §43-44: Context Budget.
 *
 * <p>Controls how the available model
 * context window is allocated across
 * different segment types. Prevents
 * memory from consuming the entire context.
 */
public record ContextBudget(
    int maxTokens,
    int memoryTokens,
    int observationTokens,
    int stateTokens,
    int instructionTokens,
    int knowledgeTokens
) {

    public ContextBudget {
        if (maxTokens <= 0) {
            maxTokens = 32_000;
        }

        if (memoryTokens <= 0) {
            memoryTokens = maxTokens / 4;
        }

        if (observationTokens <= 0) {
            observationTokens = maxTokens / 4;
        }

        if (stateTokens <= 0) {
            stateTokens = maxTokens / 8;
        }

        if (instructionTokens <= 0) {
            instructionTokens = maxTokens / 8;
        }

        if (knowledgeTokens <= 0) {
            knowledgeTokens = maxTokens / 4;
        }
    }

    /**
     * Default budget for a 32K model.
     */
    public static ContextBudget defaults() {
        return new ContextBudget(
            32_000,
            8_000,
            8_000,
            4_000,
            4_000,
            4_000
        );
    }

    /**
     * Small budget for testing.
     */
    public static ContextBudget small() {
        return new ContextBudget(
            4_000,
            1_000,
            1_000,
            500,
            500,
            500
        );
    }

    /**
     * Total allocated budget.
     */
    public int allocated() {
        return memoryTokens
            + observationTokens
            + stateTokens
            + instructionTokens
            + knowledgeTokens;
    }

    /**
     * Remaining tokens for output.
     */
    public int outputTokens() {
        return Math.max(
            0,
            maxTokens - allocated()
        );
    }
}
