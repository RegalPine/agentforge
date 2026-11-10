package io.github.regalpine.agentforge.distributed.heartbeat;

import io.github.regalpine.agentforge.distributed.worker.WorkerId;

import java.time.Instant;

public record WorkerHeartbeat(
    WorkerId workerId,
    Instant timestamp,
    int activeTasks,
    int capacity
) {
}
