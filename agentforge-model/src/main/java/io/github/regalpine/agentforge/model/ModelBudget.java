package io.github.regalpine.agentforge.model;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Model budget —
 * AF-ENHANCE-09 §57.
 *
 * <p>Tracks spending against limits:
 * token budget, cost budget,
 * time budget, invocation budget.
 *
 * <p>Thread-safe.
 */
public final class ModelBudget {

    private final long maxTokens;
    private final BigDecimal maxCost;
    private final Duration maxDuration;
    private final int maxInvocations;

    private final AtomicLong usedTokens =
        new AtomicLong(0);
    private final AtomicReference<BigDecimal>
        usedCost = new AtomicReference<>(
            BigDecimal.ZERO
        );
    private final AtomicLong invocationCount =
        new AtomicLong(0);
    private volatile long startTimeMs;

    public ModelBudget(
        long maxTokens,
        BigDecimal maxCost,
        Duration maxDuration,
        int maxInvocations
    ) {
        if (maxTokens <= 0) {
            maxTokens = Long.MAX_VALUE;
        }
        this.maxTokens = maxTokens;

        this.maxCost = maxCost == null
            || maxCost.compareTo(BigDecimal.ZERO) <= 0
            ? null : maxCost;

        this.maxDuration = maxDuration;
        this.startTimeMs = System.currentTimeMillis();

        if (maxInvocations <= 0) {
            maxInvocations = Integer.MAX_VALUE;
        }
        this.maxInvocations = maxInvocations;
    }

    /**
     * Unlimited budget.
     */
    public static ModelBudget unlimited() {
        return new ModelBudget(
            Long.MAX_VALUE, null, null,
            Integer.MAX_VALUE
        );
    }

    /**
     * Start tracking time.
     */
    public void start() {
        startTimeMs = System.currentTimeMillis();
    }

    /**
     * Check if a request is within
     * budget.
     */
    public boolean isWithinBudget() {
        if (usedTokens.get() >= maxTokens) {
            return false;
        }
        if (maxCost != null) {
            BigDecimal uc = usedCost.get();
            if (uc.compareTo(maxCost) >= 0) {
                return false;
            }
        }
        if (maxDuration != null) {
            long elapsed =
                System.currentTimeMillis()
                    - startTimeMs;
            if (Duration.ofMillis(elapsed)
                .compareTo(maxDuration) >= 0
            ) {
                return false;
            }
        }
        if (invocationCount.get()
            >= maxInvocations
        ) {
            return false;
        }
        return true;
    }

    /**
     * Record token usage.
     */
    public void recordTokens(long tokens) {
        usedTokens.addAndGet(tokens);
    }

    /**
     * Record cost.
     */
    public void recordCost(BigDecimal cost) {
        if (cost != null) {
            usedCost.updateAndGet(
                c -> c.add(cost)
            );
        }
    }

    /**
     * Record an invocation.
     */
    public void recordInvocation() {
        invocationCount.incrementAndGet();
    }

    public long maxTokens() {
        return maxTokens;
    }

    public BigDecimal maxCost() {
        return maxCost;
    }

    public Duration maxDuration() {
        return maxDuration;
    }

    public int maxInvocations() {
        return maxInvocations;
    }

    public long getUsedTokens() {
        return usedTokens.get();
    }

    public BigDecimal getUsedCost() {
        return usedCost.get();
    }

    public long getInvocationCount() {
        return invocationCount.get();
    }

    public long getRemainingTokens() {
        return Math.max(
            0, maxTokens - usedTokens.get()
        );
    }

    public Duration getElapsed() {
        return Duration.ofMillis(
            System.currentTimeMillis()
                - startTimeMs
        );
    }
}
