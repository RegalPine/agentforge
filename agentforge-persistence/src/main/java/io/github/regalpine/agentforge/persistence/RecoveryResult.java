package io.github.regalpine.agentforge.persistence;

import io.github.regalpine.agentforge.event.ExecutionState;

public record RecoveryResult(
    ExecutionState state,
    boolean recoveredFromSnapshot,
    long replayedEvents
) {
}
