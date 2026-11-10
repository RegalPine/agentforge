package io.github.regalpine.agentforge.distributed.memory;

import io.github.regalpine.agentforge.distributed.queue.TaskQueue;
import io.github.regalpine.agentforge.runtime.ExecutionTask;

import java.time.Duration;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

public final class InMemoryTaskQueue
    implements TaskQueue {

    private final LinkedBlockingQueue<
        ExecutionTask
    > queue = new LinkedBlockingQueue<>();

    private final Set<String> acknowledged =
        ConcurrentHashMap.newKeySet();

    @Override
    public void enqueue(
        ExecutionTask task
    ) {
        queue.add(task);
    }

    @Override
    public Optional<ExecutionTask> poll(
        Duration timeout
    ) {
        try {
            ExecutionTask task =
                queue.poll(
                    timeout.toMillis(),
                    TimeUnit.MILLISECONDS
                );

            return Optional.ofNullable(task);
        } catch (InterruptedException e) {
            Thread.currentThread()
                .interrupt();
            return Optional.empty();
        }
    }

    @Override
    public void acknowledge(
        String taskId
    ) {
        acknowledged.add(taskId);
    }

    @Override
    public void reject(
        String taskId
    ) {
        // In-memory: rejected tasks are simply dropped
    }

    public boolean isAcknowledged(
        String taskId
    ) {
        return acknowledged.contains(taskId);
    }

    public int size() {
        return queue.size();
    }
}
