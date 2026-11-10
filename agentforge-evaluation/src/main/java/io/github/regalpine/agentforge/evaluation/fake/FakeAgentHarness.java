package io.github.regalpine.agentforge.evaluation.fake;

import io.github.regalpine.agentforge.evaluation.harness.AgentTestHarness;
import io.github.regalpine.agentforge.evaluation.harness.ExecutionResult;
import io.github.regalpine.agentforge.evaluation.model.EvaluationInput;

import java.util.*;
import java.util.function.Function;

/**
 * FakeAgentHarness — a test double for AgentTestHarness.
 *
 * Returns pre-configured responses based on agent ID and input.
 * Useful for deterministic testing without real agent execution.
 */
public final class FakeAgentHarness implements AgentTestHarness {

    private final Map<String, Function<EvaluationInput, ExecutionResult>> responses = new HashMap<>();
    private final List<InvocationRecord> invocations = new ArrayList<>();

    /**
     * Register a fixed response for an agent.
     */
    public FakeAgentHarness withResponse(String agentId, Object output) {
        responses.put(agentId, input -> ExecutionResult.of(output));
        return this;
    }

    /**
     * Register a dynamic response for an agent.
     */
    public FakeAgentHarness withResponse(String agentId, Function<EvaluationInput, ExecutionResult> responder) {
        responses.put(agentId, responder);
        return this;
    }

    @Override
    public ExecutionResult execute(String agentId, EvaluationInput input) {
        invocations.add(new InvocationRecord(agentId, input));
        Function<EvaluationInput, ExecutionResult> responder = responses.get(agentId);
        if (responder != null) {
            return responder.apply(input);
        }
        return ExecutionResult.of(Map.of("agentId", agentId, "status", "NO_RESPONSE"));
    }

    public List<InvocationRecord> invocations() {
        return List.copyOf(invocations);
    }

    public record InvocationRecord(String agentId, EvaluationInput input) {}
}
