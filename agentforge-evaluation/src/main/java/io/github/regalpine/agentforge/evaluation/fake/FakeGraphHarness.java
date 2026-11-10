package io.github.regalpine.agentforge.evaluation.fake;

import io.github.regalpine.agentforge.evaluation.harness.GraphTestHarness;
import io.github.regalpine.agentforge.evaluation.harness.ExecutionResult;
import io.github.regalpine.agentforge.evaluation.model.EvaluationInput;

import java.util.*;
import java.util.function.Function;

/**
 * FakeGraphHarness — a test double for GraphTestHarness.
 *
 * Returns pre-configured responses based on graph ID and input.
 */
public final class FakeGraphHarness implements GraphTestHarness {

    private final Map<String, Function<EvaluationInput, ExecutionResult>> responses = new HashMap<>();
    private final List<InvocationRecord> invocations = new ArrayList<>();

    public FakeGraphHarness withResponse(String graphId, Object output) {
        responses.put(graphId, input -> ExecutionResult.of(output));
        return this;
    }

    public FakeGraphHarness withResponse(String graphId, Function<EvaluationInput, ExecutionResult> responder) {
        responses.put(graphId, responder);
        return this;
    }

    @Override
    public ExecutionResult execute(String graphId, EvaluationInput input) {
        invocations.add(new InvocationRecord(graphId, input));
        Function<EvaluationInput, ExecutionResult> responder = responses.get(graphId);
        if (responder != null) {
            return responder.apply(input);
        }
        return ExecutionResult.of(Map.of("graphId", graphId, "status", "NO_RESPONSE"));
    }

    public List<InvocationRecord> invocations() {
        return List.copyOf(invocations);
    }

    public record InvocationRecord(String graphId, EvaluationInput input) {}
}
