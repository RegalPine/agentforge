package io.github.regalpine.agentforge.persistence;

import io.github.regalpine.agentforge.kernel.EntityId;
import io.github.regalpine.agentforge.event.ExecutionId;
import io.github.regalpine.agentforge.event.ExecutionStatus;

import java.time.Instant;
import java.util.Map;

/**
 * Execution checkpoint.
 * AF-08 §13: ExecutionCheckpoint.
 *
 * <p>Captures the execution state at a point in time
 * for recovery purposes. Includes event sequence,
 * execution status, and state fingerprint for
 * precise recovery positioning.
 */
public record Checkpoint(
    String checkpointId,
    ExecutionId executionId,
    long sequence,
    long stateVersion,
    EntityId currentNode,
    ExecutionStatus status,
    Map<String, Object> data,
    String stateFingerprint,
    Instant createdAt
) {

    public Checkpoint {
        data =
            data == null
                ? Map.of()
                : Map.copyOf(data);
    }
}
