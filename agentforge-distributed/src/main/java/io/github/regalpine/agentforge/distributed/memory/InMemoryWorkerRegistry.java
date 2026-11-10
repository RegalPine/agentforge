package io.github.regalpine.agentforge.distributed.memory;

import io.github.regalpine.agentforge.distributed.worker.WorkerId;
import io.github.regalpine.agentforge.distributed.worker.WorkerRegistration;
import io.github.regalpine.agentforge.distributed.worker.WorkerRegistry;
import io.github.regalpine.agentforge.distributed.worker.WorkerStatus;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public final class InMemoryWorkerRegistry
    implements WorkerRegistry {

    private final Map<
        WorkerId,
        WorkerRegistration
    > workers = new ConcurrentHashMap<>();

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
    public Optional<WorkerRegistration> find(
        WorkerId workerId
    ) {
        return Optional.ofNullable(
            workers.get(workerId)
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
    public void heartbeat(
        WorkerId workerId
    ) {
        workers.computeIfPresent(
            workerId,
            (id, reg) -> new WorkerRegistration(
                reg.workerId(),
                reg.status(),
                reg.capabilities(),
                reg.registeredAt(),
                Instant.now()
            )
        );
    }

    @Override
    public void markDraining(
        WorkerId workerId
    ) {
        workers.computeIfPresent(
            workerId,
            (id, reg) -> new WorkerRegistration(
                reg.workerId(),
                WorkerStatus.DRAINING,
                reg.capabilities(),
                reg.registeredAt(),
                reg.lastHeartbeat()
            )
        );
    }

    @Override
    public void remove(
        WorkerId workerId
    ) {
        workers.remove(workerId);
    }
}
