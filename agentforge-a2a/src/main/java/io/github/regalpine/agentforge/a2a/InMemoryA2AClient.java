package io.github.regalpine.agentforge.a2a;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory A2A client.
 * AF-ENHANCE-10 §30.
 *
 * <p>Simulates A2A interactions
 * for testing. Real implementations
 * would communicate over HTTP/SSE.
 */
public final class InMemoryA2AClient
    implements A2AClient {

    private final Map<String, A2ATask>
        tasks = new ConcurrentHashMap<>();

    private final ExternalAgentRegistry registry;

    public InMemoryA2AClient(
        ExternalAgentRegistry registry
    ) {
        if (registry == null) {
            throw new IllegalArgumentException(
                "registry must not be null"
            );
        }

        this.registry = registry;
    }

    @Override
    public A2ATask submit(
        ExternalAgentReference agent,
        A2ARequest request
    ) {
        if (agent == null) {
            throw new IllegalArgumentException(
                "agent must not be null"
            );
        }
        if (request == null) {
            throw new IllegalArgumentException(
                "request must not be null"
            );
        }

        // Check agent exists
        AgentCard card = registry.get(
            agent.name()
        ).orElse(null);

        if (card == null) {
            throw new A2AException(
                "Agent not found: "
                    + agent.name(),
                A2AErrorCode.A2A_AGENT_NOT_FOUND
            );
        }

        // Check trust
        if (card.trust()
            == ExternalAgentTrust.REVOKED
        ) {
            throw new A2AException(
                "Agent revoked: "
                    + agent.name(),
                A2AErrorCode.A2A_PERMISSION_DENIED
            );
        }

        String taskId = "task-"
            + UUID.randomUUID();

        A2ATask task = new A2ATask(
            taskId, agent.name(),
            A2ATaskStatus.SUBMITTED,
            null, null,
            request.correlationId(),
            Instant.now(),
            Instant.now(),
            Map.of()
        );

        // Add the request message
        task = task.withMessage(
            request.message()
        );

        tasks.put(taskId, task);

        return task;
    }

    @Override
    public A2ATask getTask(
        String taskId
    ) {
        if (taskId == null) {
            return null;
        }

        A2ATask task = tasks.get(taskId);

        if (task == null) {
            throw new A2AException(
                "Task not found: " + taskId,
                A2AErrorCode.A2A_PROTOCOL_ERROR
            );
        }

        return task;
    }

    @Override
    public java.util.List<A2AEvent> stream(
        String taskId
    ) {
        A2ATask task = getTask(taskId);

        // Generate events from
        // current task state
        var events =
            new java.util.ArrayList<A2AEvent>();

        events.add(A2AEvent.of(
            taskId,
            A2AEventType.TASK_CREATED
        ));

        if (task.status()
            != A2ATaskStatus.SUBMITTED
        ) {
            events.add(A2AEvent.of(
                taskId,
                A2AEventType.TASK_STARTED
            ));
        }

        for (A2AMessage msg :
            task.messages()
        ) {
            events.add(A2AEvent.of(
                taskId,
                A2AEventType.TASK_MESSAGE,
                msg
            ));
        }

        if (task.status().isTerminal()) {
            if (task.status()
                == A2ATaskStatus.COMPLETED
            ) {
                events.add(A2AEvent.of(
                    taskId,
                    A2AEventType.TASK_COMPLETED
                ));
            } else {
                events.add(A2AEvent.of(
                    taskId,
                    A2AEventType.TASK_FAILED
                ));
            }
        }

        return List.copyOf(events);
    }

    @Override
    public void cancel(String taskId) {
        A2ATask task = getTask(taskId);

        if (task.status().isTerminal()) {
            throw new A2AException(
                "Cannot cancel terminal task",
                A2AErrorCode.A2A_TASK_CANCELLED
            );
        }

        tasks.put(
            taskId,
            task.withStatus(
                A2ATaskStatus.CANCELLED
            )
        );
    }

    /**
     * Update task status (for
     * simulation/testing).
     */
    public void updateTaskStatus(
        String taskId,
        A2ATaskStatus newStatus
    ) {
        A2ATask task = getTask(taskId);
        tasks.put(
            taskId,
            task.withStatus(newStatus)
        );
    }

    /**
     * Add artifact to task (for
     * simulation/testing).
     */
    public void addArtifact(
        String taskId,
        ArtifactReference artifact
    ) {
        A2ATask task = getTask(taskId);
        tasks.put(
            taskId,
            task.withArtifact(artifact)
        );
    }
}
