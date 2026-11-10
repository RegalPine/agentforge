package io.github.regalpine.agentforge.evaluation.replay;

import java.util.*;

/**
 * ReplayRunner — AE-12 §31-35.
 *
 * Executes replay against historical execution data.
 */
public final class ReplayRunner {

    /**
     * Replay a historical execution.
     *
     * @param data the replay data captured from a historical execution
     * @return the replay result with fidelity information
     */
    public ReplayResult replay(ReplayData data) {
        Objects.requireNonNull(data, "data");

        List<String> divergences = new ArrayList<>();
        Map<String, Object> replayedState = new HashMap<>(data.state());

        switch (data.mode()) {
            case OBSERVATION_REPLAY -> {
                // Replay observations directly
                for (ReplayData.Observation obs : data.observations()) {
                    replayedState.put("obs_" + obs.stepId(), obs.observation());
                }
            }
            case MODEL_REPLAY -> {
                // Replay model responses
                for (ReplayData.ModelResponse mr : data.modelResponses()) {
                    replayedState.put("model_" + mr.callId(), mr.response());
                }
            }
            case FULL_REPLAY -> {
                // Replay everything
                for (ReplayData.Observation obs : data.observations()) {
                    replayedState.put("obs_" + obs.stepId(), obs.observation());
                }
                for (ReplayData.ModelResponse mr : data.modelResponses()) {
                    replayedState.put("model_" + mr.callId(), mr.response());
                }
                for (ReplayData.ToolResult tr : data.toolResults()) {
                    replayedState.put("tool_" + tr.toolId(), tr.result());
                }
            }
        }

        ReplayResult.ReplayFidelity fidelity = divergences.isEmpty()
                ? ReplayResult.ReplayFidelity.EXACT
                : ReplayResult.ReplayFidelity.PARTIAL;

        return new ReplayResult(
                data.executionId(),
                data.mode(),
                fidelity,
                divergences,
                replayedState);
    }
}
