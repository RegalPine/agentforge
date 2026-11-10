package io.github.regalpine.agentforge.distributed.scheduler;

import io.github.regalpine.agentforge.distributed.queue.TaskQueue;
import io.github.regalpine.agentforge.runtime.ExecutionTask;

public final class QueueDistributedScheduler
    implements DistributedScheduler {

    private final TaskQueue taskQueue;

    public QueueDistributedScheduler(
        TaskQueue taskQueue
    ) {
        this.taskQueue = taskQueue;
    }

    @Override
    public void schedule(
        ExecutionTask task
    ) {
        taskQueue.enqueue(task);
    }
}
