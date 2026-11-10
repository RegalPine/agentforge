package io.github.regalpine.agentforge.multiagent.budget;

/**
 * Thrown when a delegation budget is exceeded.
 */
public class BudgetExceededException extends RuntimeException {

    public BudgetExceededException(String message) {
        super(message);
    }
}
