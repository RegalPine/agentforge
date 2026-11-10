package io.github.regalpine.agentforge.multiagent.budget;

/**
 * BudgetType — AE-11 §20.
 *
 * Types of budget limits that can be
 * propagated along the execution tree.
 */
public enum BudgetType {

    MAX_TOKENS,
    MAX_MODEL_CALLS,
    MAX_TOOL_CALLS,
    MAX_DURATION_MS,
    MAX_COST,
    MAX_CHILD_EXECUTIONS
}
