package io.github.regalpine.agentforge.distributed.heartbeat;

import io.github.regalpine.agentforge.distributed.worker.WorkerId;
import io.github.regalpine.agentforge.distributed.worker.WorkerRegistration;

import java.util.List;

public interface WorkerRegistrationStore {

    void register(
        WorkerRegistration registration
    );

    void heartbeat(
        WorkerHeartbeat heartbeat
    );

    List<WorkerRegistration> activeWorkers();

    boolean isAlive(
        WorkerId workerId
    );
}
