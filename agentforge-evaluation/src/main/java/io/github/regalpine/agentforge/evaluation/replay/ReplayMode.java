package io.github.regalpine.agentforge.evaluation.replay;

/**
 * ReplayMode — AE-12 §32.
 *
 * OBSERVATION_REPLAY: reuse historical observations directly.
 * MODEL_REPLAY: use historical model responses as fixed input.
 * FULL_REPLAY: restore everything possible from historical execution.
 */
public enum ReplayMode {
    OBSERVATION_REPLAY,
    MODEL_REPLAY,
    FULL_REPLAY
}
