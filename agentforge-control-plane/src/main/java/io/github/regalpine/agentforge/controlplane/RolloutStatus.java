package io.github.regalpine.agentforge.controlplane;

/**
 * Rollout lifecycle states.
 * AF-ENHANCE-14 §39-44: Rollout.
 */
public enum RolloutStatus {

    CREATED,

    IN_PROGRESS,

    PAUSED,

    COMPLETED,

    FAILED,

    ROLLED_BACK
}
