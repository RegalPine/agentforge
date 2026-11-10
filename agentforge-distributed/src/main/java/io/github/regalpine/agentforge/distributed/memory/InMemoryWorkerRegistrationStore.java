package io.github.regalpine.agentforge.distributed.memory;

import io.github.regalpine.agentforge.distributed.heartbeat.WorkerHeartbeat;
import io.github.regalpine.agentforge.distributed.heartbeat.WorkerRegistrationStore;
import io.github.regalpine.agentforge.distributed.worker.WorkerId;
import io.github.regalpine.agentforge.distributed.worker.WorkerRegistration;
import io.github.regalpine.agentforge.distributed.worker.WorkerStatus;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class InMemoryWorkerRegistrationStore
    implements WorkerRegistrationStore {

    private final Map<
        WorkerId,
        WorkerRegistration
    > workers = new ConcurrentHashMap<>();

    private final Duration heartbeatTimeout;

    public InMemoryWorkerRegistrationStore() {
        this(Duration.ofSeconds(30));
    }

    public InMemoryWorkerRegistrationStore(
        Duration heartbeatTimeout
    ) {
        this.heartbeatTimeout = heartbeatTimeout;
    }

    @Override
    public void register(
        WorkerRegistration registration
    ) {
        workers.put(
            registration.workerId(),
            registration
        );
    }

    @Override
    public void heartbeat(
        WorkerHeartbeat heartbeat
    ) {
        workers.computeIfPresent(
            heartbeat.workerId(),
            (id, reg) -> new WorkerRegistration(
                reg.workerId(),
                reg.status(),
                reg.capabilities(),
                reg.registeredAt(),
                heartbeat.timestamp()
            )
        );
    }

    @Override
    public List<WorkerRegistration> activeWorkers() {
        return workers.values()
            .stream()
            .filter(
                w -> w.status() == WorkerStatus.ACTIVE
            )
            .toList();
    }

    @Override
    public boolean isAlive(
        WorkerId workerId
    ) {
        WorkerRegistration reg =
            workers.get(workerId);

        if (reg == null) {
            return false;
        }

        if (reg.status() != WorkerStatus.ACTIVE) {
            return false;
        }

        return reg.lastHeartbeat()
            .plus(heartbeatTimeout)
            .isAfter(Instant.now());
    }
}
