package io.github.regalpine.agentforge.persistence;

import io.github.regalpine.agentforge.kernel.EntityId;
import io.github.regalpine.agentforge.kernel.Version;
import io.github.regalpine.agentforge.event.ExecutionId;

import java.time.Instant;

public record StoredInvocation(
    String invocationId,
    ExecutionId executionId,
    EntityId capabilityId,
    Version capabilityVersion,
    String idempotencyKey,
    String status,
    Instant startedAt,
    Instant completedAt,
    String resultReference
) {
}
