package io.github.regalpine.agentforge.evaluation.replay;

import java.util.*;

/**
 * ReplayResult — AE-12 §35.
 *
 * The result of a replay, including fidelity information.
 */
public final class ReplayResult {

    private final String executionId;
    private final ReplayMode mode;
    private final ReplayFidelity fidelity;
    private final List<String> divergences;
    private final Map<String, Object> replayedState;

    public ReplayResult(
            String executionId,
            ReplayMode mode,
            ReplayFidelity fidelity,
            List<String> divergences,
            Map<String, Object> replayedState) {
        this.executionId = Objects.requireNonNull(executionId, "executionId");
        this.mode = Objects.requireNonNull(mode, "mode");
        this.fidelity = Objects.requireNonNull(fidelity, "fidelity");
        this.divergences = List.copyOf(Objects.requireNonNull(divergences, "divergences"));
        this.replayedState = Map.copyOf(Objects.requireNonNull(replayedState, "replayedState"));
    }

    public String executionId() {
        return executionId;
    }

    public ReplayMode mode() {
        return mode;
    }

    public ReplayFidelity fidelity() {
        return fidelity;
    }

    public List<String> divergences() {
        return divergences;
    }

    public Map<String, Object> replayedState() {
        return replayedState;
    }

    public boolean isExact() {
        return divergences.isEmpty();
    }

    public enum ReplayFidelity {
        EXACT,
        APPROXIMATE,
        PARTIAL,
        DEGRADED
    }
}
