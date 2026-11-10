package io.github.regalpine.agentforge.distributed.lease;

import io.github.regalpine.agentforge.distributed.worker.WorkerId;
import io.github.regalpine.agentforge.event.ExecutionId;

import java.time.Instant;

public record ExecutionLease(
    ExecutionId executionId,
    WorkerId workerId,
    long fencingToken,
    Instant acquiredAt,
    Instant expiresAt
) {
}
