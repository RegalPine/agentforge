package io.github.regalpine.agentforge.runtime;

import io.github.regalpine.agentforge.event.Execution;
import io.github.regalpine.agentforge.event.ExecutionId;

import io.github.regalpine.agentforge.kernel.EntityId;
import io.github.regalpine.agentforge.kernel.Version;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public record ExecutionTask(
    String taskId,
    ExecutionId executionId,
    EntityId nodeId,
    Version graphVersion,
    String planFingerprint,
    int attempt,
    Instant createdAt
) {

    public ExecutionTask {
        Objects.requireNonNull(
            taskId,
            "taskId"
        );

        Objects.requireNonNull(
            executionId,
            "executionId"
        );

        Objects.requireNonNull(
            nodeId,
            "nodeId"
        );

        if (attempt < 1) {
            throw new IllegalArgumentException(
                "attempt must be >= 1"
            );
        }
    }

    public static ExecutionTask create(
        Execution execution,
        EntityId nodeId
    ) {
        return new ExecutionTask(
            UUID.randomUUID().toString(),
            execution.id(),
            nodeId,
            execution.graphVersion(),
            execution.planFingerprint(),
            1,
            Instant.now()
        );
    }
}
