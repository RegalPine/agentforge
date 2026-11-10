package io.github.regalpine.agentforge.model;

import java.util.Map;
import java.util.Objects;

/**
 * Token usage statistics from a model invocation.
 * <p>
 * Used for:
 * <ul>
 *   <li>Cost tracking</li>
 *   <li>Observability</li>
 *   <li>Evaluation</li>
 *   <li>Quota management</li>
 * </ul>
 */
public record TokenUsage(
    long inputTokens,
    long outputTokens,
    long totalTokens
) {

    public TokenUsage {
        if (inputTokens < 0) {
            throw new IllegalArgumentException("inputTokens cannot be negative");
        }
        if (outputTokens < 0) {
            throw new IllegalArgumentException("outputTokens cannot be negative");
        }
        if (totalTokens < 0) {
            throw new IllegalArgumentException("totalTokens cannot be negative");
        }
    }

    /**
     * Create token usage with explicit values.
     *
     * @param inputTokens  number of input tokens
     * @param outputTokens number of output tokens
     * @return a new TokenUsage
     */
    public static TokenUsage of(long inputTokens, long outputTokens) {
        return new TokenUsage(inputTokens, outputTokens, inputTokens + outputTokens);
    }

    /**
     * Create token usage with all values explicit.
     *
     * @param inputTokens  number of input tokens
     * @param outputTokens number of output tokens
     * @param totalTokens  total number of tokens
     * @return a new TokenUsage
     */
    public static TokenUsage of(long inputTokens, long outputTokens, long totalTokens) {
        return new TokenUsage(inputTokens, outputTokens, totalTokens);
    }

    /**
     * Create empty token usage.
     *
     * @return a TokenUsage with all zeros
     */
    public static TokenUsage empty() {
        return new TokenUsage(0, 0, 0);
    }

    /**
     * Add another TokenUsage to this one.
     *
     * @param other the other TokenUsage
     * @return a new TokenUsage with summed values
     */
    public TokenUsage add(TokenUsage other) {
        Objects.requireNonNull(other, "other");
        return TokenUsage.of(
            this.inputTokens + other.inputTokens,
            this.outputTokens + other.outputTokens,
            this.totalTokens + other.totalTokens
        );
    }
}
