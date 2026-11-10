package io.github.regalpine.agentforge.distributed.scheduler;

import io.github.regalpine.agentforge.distributed.worker.WorkerRegistration;
import io.github.regalpine.agentforge.runtime.ExecutionTask;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public final class RoundRobinWorkerSelector
    implements WorkerSelector {

    private final AtomicInteger counter =
        new AtomicInteger(0);

    @Override
    public WorkerRegistration select(
        ExecutionTask task,
        List<WorkerRegistration> workers
    ) {
        if (workers.isEmpty()) {
            throw new IllegalStateException(
                "No active workers available"
            );
        }

        int index =
            Math.abs(
                counter.getAndIncrement()
            ) % workers.size();

        return workers.get(index);
    }
}
