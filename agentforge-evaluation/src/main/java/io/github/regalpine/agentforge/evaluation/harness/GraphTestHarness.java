package io.github.regalpine.agentforge.evaluation.harness;

import io.github.regalpine.agentforge.evaluation.model.EvaluationInput;

/**
 * GraphTestHarness — AE-12 §26.
 *
 * Provides a controlled environment for testing graph execution.
 */
public interface GraphTestHarness {

    /**
     * Execute a graph with the given input.
     *
     * @param graphId the graph identifier
     * @param input   the evaluation input
     * @return the execution result
     */
    ExecutionResult execute(String graphId, EvaluationInput input);
}
