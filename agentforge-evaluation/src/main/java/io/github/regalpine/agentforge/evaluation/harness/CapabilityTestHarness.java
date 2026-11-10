package io.github.regalpine.agentforge.evaluation.harness;

import io.github.regalpine.agentforge.evaluation.model.EvaluationInput;

/**
 * CapabilityTestHarness — AE-12 §21.
 *
 * Provides a controlled environment for testing capabilities.
 */
public interface CapabilityTestHarness {

    /**
     * Invoke a capability with the given input.
     *
     * @param capabilityId the capability identifier
     * @param input        the evaluation input
     * @return the execution result
     */
    ExecutionResult invoke(String capabilityId, EvaluationInput input);
}
