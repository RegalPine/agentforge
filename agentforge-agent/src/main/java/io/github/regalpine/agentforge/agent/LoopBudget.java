package io.github.regalpine.agentforge.agent;

import java.time.Duration;
import java.time.Instant;
import java.util.Objects;

/**
 * Budget constraints for an Agent Loop execution.
 * <p>
 * The LoopBudget tracks multiple resource dimensions and determines
 * when the agent should stop due to budget exhaustion. Each dimension
 * can be independently configured.
 * <p>
 * All dimensions are optional — a null value means "no limit" for
 * that dimension.
 */
public record LoopBudget(
    Long maxIterations,
    Duration maxDuration,
    Long maxTokens,
    Long maxToolCalls,
    Long maxModelCalls,
    Double maxCost
) {

    public LoopBudget {
        if (maxIterations != null && maxIterations < 0) {
            throw new IllegalArgumentException("maxIterations cannot be negative");
        }
        if (maxTokens != null && maxTokens < 0) {
            throw new IllegalArgumentException("maxTokens cannot be negative");
        }
        if (maxToolCalls != null && maxToolCalls < 0) {
            throw new IllegalArgumentException("maxToolCalls cannot be negative");
        }
        if (maxModelCalls != null && maxModelCalls < 0) {
            throw new IllegalArgumentException("maxModelCalls cannot be negative");
        }
        if (maxCost != null && maxCost < 0) {
            throw new IllegalArgumentException("maxCost cannot be negative");
        }
    }

    /**
     * Create a budget with only iteration limit.
     *
     * @param maxIterations maximum number of iterations
     * @return a new LoopBudget
     */
    public static LoopBudget iterations(long maxIterations) {
        return new LoopBudget(maxIterations, null, null, null, null, null);
    }

    /**
     * Create a budget with iteration and duration limits.
     *
     * @param maxIterations maximum number of iterations
     * @param maxDuration   maximum duration
     * @return a new LoopBudget
     */
    public static LoopBudget of(long maxIterations, Duration maxDuration) {
        return new LoopBudget(maxIterations, maxDuration, null, null, null, null);
    }

    /**
     * Create a default budget with 100 iterations and 5-minute timeout.
     *
     * @return a default LoopBudget
     */
    public static LoopBudget defaults() {
        return new LoopBudget(100L, Duration.ofMinutes(5), null, null, null, null);
    }

    /**
     * Create a builder for LoopBudget.
     *
     * @return a new builder
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for LoopBudget.
     */
    public static class Builder {
        private Long maxIterations;
        private Duration maxDuration;
        private Long maxTokens;
        private Long maxToolCalls;
        private Long maxModelCalls;
        private Double maxCost;

        public Builder maxIterations(Long maxIterations) {
            this.maxIterations = maxIterations;
            return this;
        }

        public Builder maxDuration(Duration maxDuration) {
            this.maxDuration = maxDuration;
            return this;
        }

        public Builder maxTokens(Long maxTokens) {
            this.maxTokens = maxTokens;
            return this;
        }

        public Builder maxToolCalls(Long maxToolCalls) {
            this.maxToolCalls = maxToolCalls;
            return this;
        }

        public Builder maxModelCalls(Long maxModelCalls) {
            this.maxModelCalls = maxModelCalls;
            return this;
        }

        public Builder maxCost(Double maxCost) {
            this.maxCost = maxCost;
            return this;
        }

        public LoopBudget build() {
            return new LoopBudget(maxIterations, maxDuration, maxTokens, maxToolCalls, maxModelCalls, maxCost);
        }
    }
}
