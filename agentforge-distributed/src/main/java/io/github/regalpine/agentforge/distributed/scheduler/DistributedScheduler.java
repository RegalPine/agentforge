package io.github.regalpine.agentforge.distributed.scheduler;

import io.github.regalpine.agentforge.runtime.ExecutionTask;

public interface DistributedScheduler {

    void schedule(
        ExecutionTask task
    );
}
