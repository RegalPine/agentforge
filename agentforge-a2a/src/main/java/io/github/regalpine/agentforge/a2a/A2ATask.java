package io.github.regalpine.agentforge.a2a;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * A2A task.
 * AF-ENHANCE-10 §31: Task Mapping.
 *
 * <p>A2A Task ≠ Execution. It maps
 * to an external invocation tracked
 * by the A2A runtime.
 */
public record A2ATask(
    String taskId,
    String agentId,
    A2ATaskStatus status,
    List<A2AMessage> messages,
    List<ArtifactReference> artifacts,
    String correlationId,
    Instant createdAt,
    Instant updatedAt,
    Map<String, Object> metadata
) {

    public A2ATask {
        Objects.requireNonNull(
            taskId, "taskId"
        );
        Objects.requireNonNull(
            agentId, "agentId"
        );
        Objects.requireNonNull(
            status, "status"
        );

        messages = messages == null
            ? List.of() : List.copyOf(messages);
        artifacts = artifacts == null
            ? List.of() : List.copyOf(artifacts);
        correlationId = correlationId == null
            ? "" : correlationId;
        createdAt = createdAt == null
            ? Instant.now() : createdAt;
        updatedAt = updatedAt == null
            ? Instant.now() : updatedAt;
        metadata = metadata == null
            ? Map.of() : Map.copyOf(metadata);
    }

    /**
     * Create with status update.
     */
    public A2ATask withStatus(
        A2ATaskStatus newStatus
    ) {
        return new A2ATask(
            taskId, agentId, newStatus,
            messages, artifacts,
            correlationId, createdAt,
            Instant.now(), metadata
        );
    }

    /**
     * Create with added message.
     */
    public A2ATask withMessage(
        A2AMessage message
    ) {
        var newMessages =
            new java.util.ArrayList<>(messages);
        newMessages.add(message);

        return new A2ATask(
            taskId, agentId, status,
            List.copyOf(newMessages),
            artifacts, correlationId,
            createdAt, Instant.now(),
            metadata
        );
    }

    /**
     * Create with added artifact.
     */
    public A2ATask withArtifact(
        ArtifactReference artifact
    ) {
        var newArtifacts =
            new java.util.ArrayList<>(artifacts);
        newArtifacts.add(artifact);

        return new A2ATask(
            taskId, agentId, status,
            messages,
            List.copyOf(newArtifacts),
            correlationId, createdAt,
            Instant.now(), metadata
        );
    }
}
