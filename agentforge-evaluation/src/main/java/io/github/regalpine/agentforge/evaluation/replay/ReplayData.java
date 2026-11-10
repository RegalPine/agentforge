package io.github.regalpine.agentforge.evaluation.replay;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * ReplayData — AE-12 §31.
 *
 * Captures the data needed for replay from a historical execution.
 */
public final class ReplayData {

    private final String executionId;
    private final ReplayMode mode;
    private final Map<String, Object> input;
    private final Map<String, Object> state;
    private final List<ModelResponse> modelResponses;
    private final List<ToolResult> toolResults;
    private final List<Observation> observations;
    private final Map<String, String> metadata;

    public ReplayData(
            String executionId,
            ReplayMode mode,
            Map<String, Object> input,
            Map<String, Object> state,
            List<ModelResponse> modelResponses,
            List<ToolResult> toolResults,
            List<Observation> observations,
            Map<String, String> metadata) {
        this.executionId = Objects.requireNonNull(executionId, "executionId");
        this.mode = Objects.requireNonNull(mode, "mode");
        this.input = Map.copyOf(Objects.requireNonNull(input, "input"));
        this.state = Map.copyOf(Objects.requireNonNull(state, "state"));
        this.modelResponses = List.copyOf(Objects.requireNonNull(modelResponses, "modelResponses"));
        this.toolResults = List.copyOf(Objects.requireNonNull(toolResults, "toolResults"));
        this.observations = List.copyOf(Objects.requireNonNull(observations, "observations"));
        this.metadata = Map.copyOf(Objects.requireNonNull(metadata, "metadata"));
    }

    public String executionId() {
        return executionId;
    }

    public ReplayMode mode() {
        return mode;
    }

    public Map<String, Object> input() {
        return input;
    }

    public Map<String, Object> state() {
        return state;
    }

    public List<ModelResponse> modelResponses() {
        return modelResponses;
    }

    public List<ToolResult> toolResults() {
        return toolResults;
    }

    public List<Observation> observations() {
        return observations;
    }

    public Map<String, String> metadata() {
        return metadata;
    }

    public record ModelResponse(String callId, Object response) {}
    public record ToolResult(String toolId, Object result) {}
    public record Observation(String stepId, Object observation) {}
}
