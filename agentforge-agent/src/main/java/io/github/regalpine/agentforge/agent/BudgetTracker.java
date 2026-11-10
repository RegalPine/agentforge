package io.github.regalpine.agentforge.agent;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Tracks resource consumption against a LoopBudget.
 * <p>
 * Thread-safe tracker that monitors all budget dimensions and
 * reports which budget limits have been exceeded.
 */
public final class BudgetTracker {

    private final LoopBudget budget;
    private final Instant startTime;
    private final AtomicLong iterations = new AtomicLong(0);
    private final AtomicLong tokensUsed = new AtomicLong(0);
    private final AtomicLong toolCallsMade = new AtomicLong(0);
    private final AtomicLong modelCallsMade = new AtomicLong(0);
    private final AtomicReference<Double> costAccumulated = new AtomicReference<>(0.0);

    public BudgetTracker(LoopBudget budget) {
        this.budget = budget != null ? budget : LoopBudget.defaults();
        this.startTime = Instant.now();
    }

    /**
     * Record the start of a new iteration.
     *
     * @return the iteration number (1-based)
     */
    public long recordIteration() {
        return iterations.incrementAndGet();
    }

    /**
     * Record token usage.
     *
     * @param tokens number of tokens used
     */
    public void recordTokens(long tokens) {
        if (tokens > 0) {
            tokensUsed.addAndGet(tokens);
        }
    }

    /**
     * Record a tool call.
     *
     * @return the tool call number (1-based)
     */
    public long recordToolCall() {
        return toolCallsMade.incrementAndGet();
    }

    /**
     * Record a model call.
     *
     * @return the model call number (1-based)
     */
    public long recordModelCall() {
        return modelCallsMade.incrementAndGet();
    }

    /**
     * Record cost.
     *
     * @param cost the cost amount
     */
    public void recordCost(double cost) {
        if (cost > 0) {
            costAccumulated.updateAndGet(current -> current + cost);
        }
    }

    /**
     * Check if any budget limit has been exceeded.
     *
     * @return an Optional containing the StopCondition if budget is exhausted, empty otherwise
     */
    public Optional<StopCondition> checkBudget() {
        if (budget.maxIterations() != null && iterations.get() >= budget.maxIterations()) {
            return Optional.of(StopCondition.of(
                StopCondition.StopReason.MAX_ITERATIONS,
                "Reached max iterations: " + budget.maxIterations()
            ));
        }
        if (budget.maxDuration() != null) {
            Duration elapsed = Duration.between(startTime, Instant.now());
            if (elapsed.compareTo(budget.maxDuration()) >= 0) {
                return Optional.of(StopCondition.of(
                    StopCondition.StopReason.TIMEOUT,
                    "Exceeded max duration: " + budget.maxDuration()
                ));
            }
        }
        if (budget.maxTokens() != null && tokensUsed.get() >= budget.maxTokens()) {
            return Optional.of(StopCondition.of(
                StopCondition.StopReason.TOKEN_BUDGET_EXHAUSTED,
                "Token budget exhausted: " + tokensUsed.get() + " >= " + budget.maxTokens()
            ));
        }
        if (budget.maxToolCalls() != null && toolCallsMade.get() >= budget.maxToolCalls()) {
            return Optional.of(StopCondition.of(
                StopCondition.StopReason.TOOL_BUDGET_EXHAUSTED,
                "Tool call budget exhausted: " + toolCallsMade.get() + " >= " + budget.maxToolCalls()
            ));
        }
        if (budget.maxModelCalls() != null && modelCallsMade.get() >= budget.maxModelCalls()) {
            return Optional.of(StopCondition.of(
                StopCondition.StopReason.MODEL_BUDGET_EXHAUSTED,
                "Model call budget exhausted: " + modelCallsMade.get() + " >= " + budget.maxModelCalls()
            ));
        }
        if (budget.maxCost() != null && costAccumulated.get() >= budget.maxCost()) {
            return Optional.of(StopCondition.of(
                StopCondition.StopReason.COST_BUDGET_EXHAUSTED,
                "Cost budget exhausted: " + costAccumulated.get() + " >= " + budget.maxCost()
            ));
        }
        return Optional.empty();
    }

    public long getIterations() {
        return iterations.get();
    }

    public long getTokensUsed() {
        return tokensUsed.get();
    }

    public long getToolCallsMade() {
        return toolCallsMade.get();
    }

    public long getModelCallsMade() {
        return modelCallsMade.get();
    }

    public double getCostAccumulated() {
        return costAccumulated.get();
    }

    public Duration getElapsed() {
        return Duration.between(startTime, Instant.now());
    }

    public LoopBudget getBudget() {
        return budget;
    }
}
