package io.github.regalpine.agentforge.persistence;

import io.github.regalpine.agentforge.event.Execution;
import io.github.regalpine.agentforge.event.ExecutionState;
import io.github.regalpine.agentforge.event.ExecutionStatus;
import io.github.regalpine.agentforge.event.ExecutionId;
import io.github.regalpine.agentforge.event.ExecutionEvent;

import io.github.regalpine.agentforge.graph.ir.ExecutionNode;
import io.github.regalpine.agentforge.graph.ir.ExecutionPlan;
import io.github.regalpine.agentforge.graph.NodeType;
import io.github.regalpine.agentforge.kernel.EntityId;
import io.github.regalpine.agentforge.kernel.Version;
import io.github.regalpine.agentforge.persistence.memory.*;
import io.github.regalpine.agentforge.runtime.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class RecoveryManagerTest {

    private InMemoryExecutionStore executionStore;
    private InMemoryExecutionStateStore stateStore;
    private InMemorySnapshotStore snapshotStore;
    private InMemoryEventStore eventStore;
    private InMemoryExecutionPlanRegistry planRegistry;

    private final ExecutionId execId =
        new ExecutionId("exec-recovery");

    private final EntityId graphId =
        new EntityId("test-graph");

    private final Version graphVersion =
        new Version("1.0.0");

    private final String fingerprint = "sha256-test";

    @BeforeEach
    void setUp() {
        executionStore = new InMemoryExecutionStore();
        stateStore = new InMemoryExecutionStateStore();
        snapshotStore = new InMemorySnapshotStore();
        eventStore = new InMemoryEventStore();
        planRegistry = new InMemoryExecutionPlanRegistry();
    }

    private RecoveryManager createRecoveryManager() {
        StateReducer reducer = (state, event) -> {
            String type = event.type();

            if ("NODE_COMPLETED".equals(type)) {
                String nodeId = (String) event.payload()
                    .getOrDefault("nodeId", "");

                Set<EntityId> completed =
                    new java.util.HashSet<>(
                        state.completedNodes()
                    );

                completed.add(new EntityId(nodeId));

                return new ExecutionState(
                    state.executionId(),
                    state.status(),
                    null,
                    state.data(),
                    Set.copyOf(completed),
                    state.waitingNodes(),
                    state.version() + 1
                );
            }

            return state;
        };

        return new DefaultRecoveryManager(
            executionStore,
            stateStore,
            snapshotStore,
            eventStore,
            planRegistry,
            reducer
        );
    }

    @Test
    void shouldRecoverFromStateWithEvents() {
        // Setup execution
        Execution execution = new Execution(
            execId, graphId, graphVersion,
            fingerprint,
            ExecutionStatus.RUNNING,
            Instant.now(),
            Instant.now(),
            null
        );

        executionStore.create(execution);

        // Setup state
        ExecutionState state = new ExecutionState(
            execId,
            ExecutionStatus.RUNNING,
            new EntityId("capability-1"),
            Map.of("result", 42),
            Set.of(new EntityId("start")),
            Set.of(),
            1
        );

        stateStore.create(state);

        // Setup plan
        ExecutionPlan plan = new ExecutionPlan(
            graphId, graphVersion,
            fingerprint,
            new EntityId("start"),
            List.of(
                new ExecutionNode(
                    new EntityId("start"),
                    NodeType.START,
                    Map.of(), Map.of()
                ),
                new ExecutionNode(
                    new EntityId("capability-1"),
                    NodeType.CAPABILITY,
                    Map.of(), Map.of()
                ),
                new ExecutionNode(
                    new EntityId("end"),
                    NodeType.END,
                    Map.of(), Map.of()
                )
            ),
            Map.of(),
            Map.of()
        );

        planRegistry.register(plan);

        // Add events after initial state
        ExecutionEvent event = new ExecutionEvent(
            execId, 1, Instant.now(),
            "NODE_COMPLETED",
            Map.of("nodeId", "capability-1")
        );

        eventStore.append(event);

        // Recover
        RecoveryManager recovery =
            createRecoveryManager();

        RecoveryResult result =
            recovery.recover(execId);

        assertNotNull(result);
        assertFalse(result.recoveredFromSnapshot());
        assertEquals(1, result.replayedEvents());

        // State should have capability-1 in completedNodes
        assertTrue(
            result.state().completedNodes().contains(
                new EntityId("capability-1")
            )
        );
    }

    @Test
    void shouldRecoverFromSnapshot() {
        Execution execution = new Execution(
            execId, graphId, graphVersion,
            fingerprint,
            ExecutionStatus.RUNNING,
            Instant.now(),
            Instant.now(),
            null
        );

        executionStore.create(execution);

        // Setup snapshot
        Snapshot snapshot = new Snapshot(
            "snap-1", execId, 5, 3,
            new EntityId("node-start"),
            io.github.regalpine.agentforge.event.ExecutionStatus.RUNNING,
            Map.of("data", "snapshot-value"),
            null,
            Instant.now()
        );

        snapshotStore.save(snapshot);

        ExecutionPlan plan = new ExecutionPlan(
            graphId, graphVersion,
            fingerprint,
            new EntityId("start"),
            List.of(),
            Map.of(),
            Map.of()
        );

        planRegistry.register(plan);

        // Add events after snapshot
        ExecutionEvent event = new ExecutionEvent(
            execId, 6, Instant.now(),
            "NODE_COMPLETED",
            Map.of("nodeId", "node-4")
        );

        eventStore.append(event);

        RecoveryManager recovery =
            createRecoveryManager();

        RecoveryResult result =
            recovery.recover(execId);

        assertTrue(result.recoveredFromSnapshot());
        assertEquals(1, result.replayedEvents());
    }

    @Test
    void shouldRejectMismatchedFingerprint() {
        Execution execution = new Execution(
            execId, graphId, graphVersion,
            "sha256-OLD",
            ExecutionStatus.RUNNING,
            Instant.now(),
            Instant.now(),
            null
        );

        executionStore.create(execution);

        ExecutionPlan plan = new ExecutionPlan(
            graphId, graphVersion,
            "sha256-NEW",
            new EntityId("start"),
            List.of(),
            Map.of(),
            Map.of()
        );

        planRegistry.register(plan);

        RecoveryManager recovery =
            createRecoveryManager();

        assertThrows(
            IllegalStateException.class,
            () -> recovery.recover(execId)
        );
    }

    @Test
    void shouldRejectMissingExecution() {
        RecoveryManager recovery =
            createRecoveryManager();

        assertThrows(
            IllegalStateException.class,
            () -> recovery.recover(
                new ExecutionId("nonexistent")
            )
        );
    }
}
