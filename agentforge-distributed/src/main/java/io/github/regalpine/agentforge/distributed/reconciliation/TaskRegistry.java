package io.github.regalpine.agentforge.distributed.reconciliation;

import io.github.regalpine.agentforge.event.ExecutionId;
import io.github.regalpine.agentforge.runtime.ExecutionTask;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public final class TaskRegistry {

    private final Map<
        String,
        ExecutionTask
    > tasks = new ConcurrentHashMap<>();

    public void register(
        ExecutionTask task
    ) {
        tasks.put(
            task.taskId(),
            task
        );
    }

    public boolean hasPendingTask(
        ExecutionId executionId
    ) {
        return tasks.values()
            .stream()
            .anyMatch(
                task -> task.executionId()
                    .equals(executionId)
            );
    }

    public Set<String> getTaskNodeIds(
        ExecutionId executionId
    ) {
        return tasks.values()
            .stream()
            .filter(
                task -> task.executionId()
                    .equals(executionId)
            )
            .map(
                task -> task.nodeId().value()
            )
            .collect(
                Collectors.toUnmodifiableSet()
            );
    }

    public void removeTasks(
        ExecutionId executionId
    ) {
        tasks.entrySet()
            .removeIf(
                entry -> entry.getValue()
                    .executionId()
                    .equals(executionId)
            );
    }

    public void removeTaskForNode(
        ExecutionId executionId,
        String nodeId
    ) {
        tasks.entrySet()
            .removeIf(
                entry -> {
                    ExecutionTask task =
                        entry.getValue();

                    return task.executionId()
                        .equals(executionId)
                        && task.nodeId()
                            .value()
                            .equals(nodeId);
                }
            );
    }

    public void remove(
        String taskId
    ) {
        tasks.remove(taskId);
    }

    public Set<String> pendingTaskIds() {
        return tasks.keySet();
    }
}
