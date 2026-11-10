package io.github.regalpine.agentforge.evaluation.fake;

import io.github.regalpine.agentforge.evaluation.harness.CapabilityTestHarness;
import io.github.regalpine.agentforge.evaluation.harness.ExecutionResult;
import io.github.regalpine.agentforge.evaluation.model.EvaluationInput;

import java.util.*;
import java.util.function.Function;

/**
 * FakeCapabilityHarness — a test double for CapabilityTestHarness.
 *
 * Supports simulating: SUCCESS, FAILURE, TIMEOUT, POLICY_DENIED, INVALID_OUTPUT.
 */
public final class FakeCapabilityHarness implements CapabilityTestHarness {

    private final Map<String, Function<EvaluationInput, ExecutionResult>> responses = new HashMap<>();
    private final List<InvocationRecord> invocations = new ArrayList<>();

    public FakeCapabilityHarness withResponse(String capabilityId, Object output) {
        responses.put(capabilityId, input -> ExecutionResult.of(output));
        return this;
    }

    public FakeCapabilityHarness withResponse(String capabilityId, Function<EvaluationInput, ExecutionResult> responder) {
        responses.put(capabilityId, responder);
        return this;
    }

    public FakeCapabilityHarness withFailure(String capabilityId, String errorMessage) {
        responses.put(capabilityId, input -> {
            throw new RuntimeException(errorMessage);
        });
        return this;
    }

    public FakeCapabilityHarness withTimeout(String capabilityId) {
        responses.put(capabilityId, input -> {
            throw new RuntimeException("TIMEOUT");
        });
        return this;
    }

    @Override
    public ExecutionResult invoke(String capabilityId, EvaluationInput input) {
        invocations.add(new InvocationRecord(capabilityId, input));
        Function<EvaluationInput, ExecutionResult> responder = responses.get(capabilityId);
        if (responder != null) {
            return responder.apply(input);
        }
        return ExecutionResult.of(Map.of("capabilityId", capabilityId, "status", "NO_RESPONSE"));
    }

    public List<InvocationRecord> invocations() {
        return List.copyOf(invocations);
    }

    public record InvocationRecord(String capabilityId, EvaluationInput input) {}
}
