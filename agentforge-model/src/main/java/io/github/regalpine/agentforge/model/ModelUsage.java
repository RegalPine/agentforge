package io.github.regalpine.agentforge.model;

import java.util.Map;
import java.util.Objects;

/**
 * Enhanced token usage from a model invocation.
 * <p>
 * Extends the basic {@link TokenUsage} with additional fields for
 * cached input tokens, reasoning tokens, and provider-specific usage data.
 */
public record ModelUsage(
    long inputTokens,
    long outputTokens,
    long totalTokens,
    long cachedInputTokens,
    long reasoningTokens,
    Map<String, Object> providerUsage
) {

    public ModelUsage {
        if (inputTokens < 0) {
            throw new IllegalArgumentException("inputTokens cannot be negative");
        }
        if (outputTokens < 0) {
            throw new IllegalArgumentException("outputTokens cannot be negative");
        }

        providerUsage = providerUsage == null ? Map.of() : Map.copyOf(providerUsage);
    }

    /**
     * Create model usage with basic token counts.
     *
     * @param inputTokens  number of input tokens
     * @param outputTokens number of output tokens
     * @return a new ModelUsage
     */
    public static ModelUsage of(long inputTokens, long outputTokens) {
        return new ModelUsage(inputTokens, outputTokens, inputTokens + outputTokens, 0, 0, null);
    }

    /**
     * Create model usage with all standard fields.
     *
     * @param inputTokens        number of input tokens
     * @param outputTokens       number of output tokens
     * @param cachedInputTokens  number of cached input tokens
     * @param reasoningTokens    number of reasoning tokens
     * @return a new ModelUsage
     */
    public static ModelUsage of(long inputTokens, long outputTokens, long cachedInputTokens, long reasoningTokens) {
        return new ModelUsage(inputTokens, outputTokens, inputTokens + outputTokens, cachedInputTokens, reasoningTokens, null);
    }

    /**
     * Create empty model usage.
     *
     * @return a ModelUsage with all zeros
     */
    public static ModelUsage empty() {
        return new ModelUsage(0, 0, 0, 0, 0, null);
    }

    /**
     * Convert to basic TokenUsage.
     *
     * @return a TokenUsage with the basic token counts
     */
    public TokenUsage toTokenUsage() {
        return TokenUsage.of(inputTokens, outputTokens, totalTokens);
    }

    /**
     * Add another ModelUsage to this one.
     *
     * @param other the other ModelUsage
     * @return a new ModelUsage with summed values
     */
    public ModelUsage add(ModelUsage other) {
        Objects.requireNonNull(other, "other");
        return new ModelUsage(
            this.inputTokens + other.inputTokens,
            this.outputTokens + other.outputTokens,
            this.totalTokens + other.totalTokens,
            this.cachedInputTokens + other.cachedInputTokens,
            this.reasoningTokens + other.reasoningTokens,
            Map.of()
        );
    }
}
