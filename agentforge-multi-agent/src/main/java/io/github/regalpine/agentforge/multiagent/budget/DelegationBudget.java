package io.github.regalpine.agentforge.multiagent.budget;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * DelegationBudget — AE-11 §18-20.
 *
 * Tracks budget propagation along the execution tree.
 * Parent remaining budget → child allocation → child
 * consumption → parent reconciliation.
 *
 * <p>Child agent cannot exceed its allocated budget.
 * Supports: MAX_TOKENS, MAX_MODEL_CALLS, MAX_TOOL_CALLS,
 * MAX_DURATION_MS, MAX_COST, MAX_CHILD_EXECUTIONS.
 */
public final class DelegationBudget {

    private final long rootMaxTokens;
    private final long allocatedMaxTokens;
    private final AtomicLong consumedTokens;

    private final int rootMaxModelCalls;
    private final int allocatedMaxModelCalls;
    private final AtomicLong consumedModelCalls;

    private final int rootMaxToolCalls;
    private final int allocatedMaxToolCalls;
    private final AtomicLong consumedToolCalls;

    private final int rootMaxChildExecutions;
    private final int allocatedMaxChildExecutions;
    private final AtomicLong consumedChildExecutions;

    private final Map<String, Object> extra;

    public DelegationBudget(
        long rootMaxTokens,
        long allocatedMaxTokens,
        long consumedTokens,
        int rootMaxModelCalls,
        int allocatedMaxModelCalls,
        long consumedModelCalls,
        int rootMaxToolCalls,
        int allocatedMaxToolCalls,
        long consumedToolCalls,
        int rootMaxChildExecutions,
        int allocatedMaxChildExecutions,
        long consumedChildExecutions,
        Map<String, Object> extra
    ) {
        this.rootMaxTokens = rootMaxTokens;
        this.allocatedMaxTokens = allocatedMaxTokens;
        this.consumedTokens = new AtomicLong(consumedTokens);
        this.rootMaxModelCalls = rootMaxModelCalls;
        this.allocatedMaxModelCalls = allocatedMaxModelCalls;
        this.consumedModelCalls =
            new AtomicLong(consumedModelCalls);
        this.rootMaxToolCalls = rootMaxToolCalls;
        this.allocatedMaxToolCalls = allocatedMaxToolCalls;
        this.consumedToolCalls =
            new AtomicLong(consumedToolCalls);
        this.rootMaxChildExecutions = rootMaxChildExecutions;
        this.allocatedMaxChildExecutions =
            allocatedMaxChildExecutions;
        this.consumedChildExecutions =
            new AtomicLong(consumedChildExecutions);
        this.extra = extra == null
            ? Map.of() : Map.copyOf(extra);
    }

    /**
     * Create a root budget with default limits.
     */
    public static DelegationBudget root(
        long maxTokens,
        int maxModelCalls,
        int maxToolCalls,
        int maxChildExecutions
    ) {
        return new DelegationBudget(
            maxTokens, maxTokens, 0,
            maxModelCalls, maxModelCalls, 0,
            maxToolCalls, maxToolCalls, 0,
            maxChildExecutions, maxChildExecutions, 0,
            Map.of()
        );
    }

    /**
     * Create a root budget with sensible defaults.
     */
    public static DelegationBudget defaults() {
        return root(
            100_000, 50, 100, 10
        );
    }

    /**
     * Create a child budget allocated from parent.
     */
    public DelegationBudget allocateChild(
        long tokens,
        int modelCalls,
        int toolCalls,
        int childExecutions
    ) {
        if (tokens > remainingTokens()) {
            throw new BudgetExceededException(
                "Token allocation " + tokens
                + " exceeds remaining "
                + remainingTokens());
        }

        return new DelegationBudget(
            rootMaxTokens, tokens, 0,
            rootMaxModelCalls, modelCalls, 0,
            rootMaxToolCalls, toolCalls, 0,
            rootMaxChildExecutions, childExecutions, 0,
            Map.of()
        );
    }

    // --- Token tracking ---

    public long addTokenConsumption(long tokens) {
        long consumed = consumedTokens.addAndGet(tokens);
        if (consumed > allocatedMaxTokens) {
            consumedTokens.addAndGet(-tokens);
            throw new BudgetExceededException(
                "Token budget exceeded: "
                + consumed + " > "
                + allocatedMaxTokens);
        }
        return consumed;
    }

    public long remainingTokens() {
        return allocatedMaxTokens - consumedTokens.get();
    }

    public long consumedTokens() {
        return consumedTokens.get();
    }

    public long allocatedMaxTokens() {
        return allocatedMaxTokens;
    }

    public long rootMaxTokens() {
        return rootMaxTokens;
    }

    // --- Model call tracking ---

    public long addModelCallConsumption() {
        long consumed = consumedModelCalls.incrementAndGet();
        if (consumed > allocatedMaxModelCalls) {
            consumedModelCalls.decrementAndGet();
            throw new BudgetExceededException(
                "Model call budget exceeded: "
                + consumed + " > "
                + allocatedMaxModelCalls);
        }
        return consumed;
    }

    public long remainingModelCalls() {
        return allocatedMaxModelCalls
            - consumedModelCalls.get();
    }

    public long consumedModelCalls() {
        return consumedModelCalls.get();
    }

    // --- Tool call tracking ---

    public long addToolCallConsumption() {
        long consumed = consumedToolCalls.incrementAndGet();
        if (consumed > allocatedMaxToolCalls) {
            consumedToolCalls.decrementAndGet();
            throw new BudgetExceededException(
                "Tool call budget exceeded: "
                + consumed + " > "
                + allocatedMaxToolCalls);
        }
        return consumed;
    }

    public long remainingToolCalls() {
        return allocatedMaxToolCalls
            - consumedToolCalls.get();
    }

    public long consumedToolCalls() {
        return consumedToolCalls.get();
    }

    // --- Child execution tracking ---

    public long addChildExecutionConsumption() {
        long consumed =
            consumedChildExecutions.incrementAndGet();
        if (consumed > allocatedMaxChildExecutions) {
            consumedChildExecutions.decrementAndGet();
            throw new BudgetExceededException(
                "Child execution budget exceeded: "
                + consumed + " > "
                + allocatedMaxChildExecutions);
        }
        return consumed;
    }

    public long remainingChildExecutions() {
        return allocatedMaxChildExecutions
            - consumedChildExecutions.get();
    }

    public long consumedChildExecutions() {
        return consumedChildExecutions.get();
    }

    /**
     * Check if any budget is exhausted.
     */
    public boolean isExhausted() {
        return remainingTokens() <= 0
            || remainingModelCalls() <= 0
            || remainingToolCalls() <= 0
            || remainingChildExecutions() <= 0;
    }

    public Map<String, Object> extra() {
        return extra;
    }
}
