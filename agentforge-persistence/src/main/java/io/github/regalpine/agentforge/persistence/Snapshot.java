package io.github.regalpine.agentforge.persistence;

import io.github.regalpine.agentforge.kernel.EntityId;
import io.github.regalpine.agentforge.event.ExecutionId;
import io.github.regalpine.agentforge.event.ExecutionStatus;

import java.time.Instant;
import java.util.Map;

/**
 * Execution snapshot.
 * AF-08 §23: ExecutionSnapshot.
 *
 * <p>Full state snapshot for recovery. Includes
 * current node, execution status, and state
 * fingerprint for precise recovery positioning.
 */
public record Snapshot(
    String snapshotId,
    ExecutionId executionId,
    long eventSequence,
    long stateVersion,
    EntityId currentNodeId,
    ExecutionStatus status,
    Map<String, Object> state,
    String stateFingerprint,
    Instant createdAt
) {

    public Snapshot {
        state =
            state == null
                ? Map.of()
                : Map.copyOf(state);
    }
}
