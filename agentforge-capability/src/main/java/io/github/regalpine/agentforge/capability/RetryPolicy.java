package io.github.regalpine.agentforge.capability;

import java.time.Duration;

/**
 * Retry policy for capability invocations.
 * AE-02 §27: RetryPolicy.
 *
 * <p>Different capabilities may have different
 * retry strategies. For example, LLM calls can
 * retry on transient errors, while payment tools
 * should not auto-retry.
 */
public record RetryPolicy(
    int maxRetries,
    Duration initialDelay,
    double backoffMultiplier,
    Duration maxDelay
) {

    public RetryPolicy {
        if (maxRetries < 0) {
            throw new IllegalArgumentException(
                "maxRetries must be >= 0"
            );
        }

        initialDelay =
            initialDelay == null
                ? Duration.ofMillis(100)
                : initialDelay;

        if (backoffMultiplier < 1.0) {
            backoffMultiplier = 2.0;
        }

        maxDelay =
            maxDelay == null
                ? Duration.ofSeconds(10)
                : maxDelay;
    }

    /**
     * No retry — fail immediately.
     */
    public static RetryPolicy none() {
        return new RetryPolicy(
            0,
            Duration.ofMillis(100),
            2.0,
            Duration.ofSeconds(10)
        );
    }

    /**
     * Fixed number of retries with default
     * exponential backoff.
     */
    public static RetryPolicy fixed(int maxRetries) {
        return new RetryPolicy(
            maxRetries,
            Duration.ofMillis(100),
            2.0,
            Duration.ofSeconds(10)
        );
    }

    /**
     * Compute delay for the given attempt
     * number (0-based).
     */
    public Duration delayForAttempt(int attempt) {
        double delayMs =
            initialDelay.toMillis()
                * Math.pow(backoffMultiplier, attempt);

        return Duration.ofMillis(
            (long) Math.min(delayMs, maxDelay.toMillis())
        );
    }
}
