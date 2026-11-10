package io.github.regalpine.agentforge.distributed.worker;

import java.time.Instant;
import java.util.Set;

public record WorkerRegistration(
    WorkerId workerId,
    WorkerStatus status,
    Set<String> capabilities,
    Instant registeredAt,
    Instant lastHeartbeat
) {

    public WorkerRegistration {
        capabilities =
            capabilities == null
                ? Set.of()
                : Set.copyOf(capabilities);
    }
}
