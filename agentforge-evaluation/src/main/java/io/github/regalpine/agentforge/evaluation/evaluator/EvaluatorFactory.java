package io.github.regalpine.agentforge.evaluation.evaluator;

import io.github.regalpine.agentforge.evaluation.model.OutcomeMode;

/**
 * EvaluatorFactory — creates the appropriate Evaluator based on OutcomeMode.
 */
public final class EvaluatorFactory {

    private EvaluatorFactory() {}

    /**
     * Creates an Evaluator for the given outcome mode.
     *
     * @param mode the outcome mode
     * @return the appropriate evaluator
     */
    public static Evaluator create(OutcomeMode mode) {
        return switch (mode) {
            case EXACT -> new ExactEvaluator();
            case STRUCTURAL -> new StructuralEvaluator();
            case SEMANTIC -> SemanticEvaluator.withTokenOverlap();
            case BEHAVIORAL -> new BehavioralEvaluator();
            case POLICY -> new PolicyEvaluator();
            case CUSTOM -> new ExactEvaluator(); // fallback
        };
    }
}
