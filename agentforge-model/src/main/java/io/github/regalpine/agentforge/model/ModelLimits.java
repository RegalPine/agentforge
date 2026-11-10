package io.github.regalpine.agentforge.model;

import java.util.Objects;

/**
 * Model limits — AF-ENHANCE-09 §41.
 *
 * <p>Declares the context window
 * constraints for a model.
 *
 * <p>Runtime must validate request
 * size before invocation.
 */
public record ModelLimits(
    long maxInputTokens,
    long maxOutputTokens,
    long maxContextTokens
) {

    public ModelLimits {
        if (maxInputTokens < 0) {
            throw new IllegalArgumentException(
                "maxInputTokens cannot be negative"
            );
        }
        if (maxOutputTokens < 0) {
            throw new IllegalArgumentException(
                "maxOutputTokens cannot be negative"
            );
        }
        if (maxContextTokens < 0) {
            maxContextTokens =
                maxInputTokens + maxOutputTokens;
        }
    }

    /**
     * Default limits: 128K context.
     */
    public static ModelLimits defaults() {
        return new ModelLimits(
            128_000, 4_096, 132_096
        );
    }

    /**
     * Small model limits.
     */
    public static ModelLimits small() {
        return new ModelLimits(
            8_000, 2_048, 10_248
        );
    }

    /**
     * Large model limits.
     */
    public static ModelLimits large() {
        return new ModelLimits(
            1_000_000, 16_384, 1_016_384
        );
    }

    /**
     * Check if a token count fits
     * within the context window.
     */
    public boolean fits(long inputTokens) {
        return inputTokens <= maxInputTokens;
    }

    /**
     * Check if input + output fits.
     */
    public boolean fits(
        long inputTokens,
        long outputTokens
    ) {
        return inputTokens <= maxInputTokens
            && outputTokens <= maxOutputTokens
            && (inputTokens + outputTokens)
                <= maxContextTokens;
    }
}
