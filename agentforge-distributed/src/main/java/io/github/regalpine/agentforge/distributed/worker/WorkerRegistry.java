package io.github.regalpine.agentforge.distributed.worker;

import java.util.List;
import java.util.Optional;

public interface WorkerRegistry {

    void register(
        WorkerRegistration registration
    );

    Optional<WorkerRegistration> find(
        WorkerId workerId
    );

    List<WorkerRegistration> activeWorkers();

    void heartbeat(
        WorkerId workerId
    );

    void markDraining(
        WorkerId workerId
    );

    void remove(
        WorkerId workerId
    );
}
