package io.github.regalpine.agentforge.distributed.queue;

import io.github.regalpine.agentforge.runtime.ExecutionTask;

import java.time.Duration;
import java.util.Optional;

public interface TaskQueue {

    void enqueue(
        ExecutionTask task
    );

    Optional<ExecutionTask> poll(
        Duration timeout
    );

    void acknowledge(
        String taskId
    );

    void reject(
        String taskId
    );
}
