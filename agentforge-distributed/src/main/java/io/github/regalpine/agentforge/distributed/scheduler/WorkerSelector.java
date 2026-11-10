package io.github.regalpine.agentforge.distributed.scheduler;

import io.github.regalpine.agentforge.distributed.worker.WorkerRegistration;
import io.github.regalpine.agentforge.runtime.ExecutionTask;

import java.util.List;

public interface WorkerSelector {

    WorkerRegistration select(
        ExecutionTask task,
        List<WorkerRegistration> workers
    );
}
