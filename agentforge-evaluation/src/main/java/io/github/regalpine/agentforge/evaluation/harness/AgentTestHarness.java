package io.github.regalpine.agentforge.evaluation.harness;

import io.github.regalpine.agentforge.evaluation.model.EvaluationInput;

/**
 * AgentTestHarness — AE-12 §19.
 *
 * Provides a controlled environment for testing agents.
 * The harness can control: Model, Tool, Memory, RAG, MCP, Clock, Randomness, Budget, Policy.
 */
public interface AgentTestHarness {

    /**
     * Execute an agent with the given input.
     *
     * @param agentId the agent identifier
     * @param input   the evaluation input
     * @return the execution result
     */
    ExecutionResult execute(String agentId, EvaluationInput input);
}
