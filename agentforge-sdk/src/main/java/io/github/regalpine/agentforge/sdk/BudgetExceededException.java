package io.github.regalpine.agentforge.sdk;

/** Thrown when an execution exceeds its budget. */
public class BudgetExceededException extends AgentForgeException {
    public BudgetExceededException(String message) {
        super("BUDGET_EXCEEDED", message, null, null, null, null, false, java.util.Map.of());
    }
}
