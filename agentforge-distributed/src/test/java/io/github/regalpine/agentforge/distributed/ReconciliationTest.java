package io.github.regalpine.agentforge.distributed;

import io.github.regalpine.agentforge.event.Execution;
import io.github.regalpine.agentforge.event.ExecutionState;
import io.github.regalpine.agentforge.event.ExecutionStatus;
import io.github.regalpine.agentforge.event.ExecutionId;

import io.github.regalpine.agentforge.distributed.lease.LeaseStore;
import io.github.regalpine.agentforge.distributed.memory.InMemoryLeaseStore;
import io.github.regalpine.agentforge.distributed.reconciliation.DefaultReconciler;
import io.github.regalpine.agentforge.distributed.reconciliation.ReconciliationResult;
import io.github.regalpine.agentforge.distributed.reconciliation.TaskRegistry;
import io.github.regalpine.agentforge.distributed.worker.WorkerId;
import io.github.regalpine.agentforge.kernel.EntityId;
import io.github.regalpine.agentforge.kernel.Version;
import io.github.regalpine.agentforge.runtime.*;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ReconciliationTest {

    @Test
    void shouldRecreateTaskWhenLeaseExpiredAndNoTask() {

        InMemoryStateManager stateManager =
            new InMemoryStateManager();

        InMemoryLeaseStore leaseStore =
            new InMemoryLeaseStore();

        TaskRegistry taskRegistry =
            new TaskRegistry();

        ExecutionId execId =
            new ExecutionId("exec-recon-001");

        WorkerId workerA =
            new WorkerId("worker-A");

        Execution execution =
            new Execution(
                execId,
                new EntityId("graph-1"),
                new Version("1.0.0"),
                "fp-001",
                ExecutionStatus.READY,
                Instant.now(),
                null,
                null
            );

        ExecutionState state =
            new ExecutionState(
                execId,
                ExecutionStatus.RUNNING,
                new EntityId("calc"),
                Map.of(),
                java.util.Set.of(new EntityId("start")),
                java.util.Set.of(),
                2
            );

        stateManager.create(state);

        leaseStore.acquire(
            execId,
            workerA,
            1
        );

        List<Execution> tracked =
            new ArrayList<>();

        tracked.add(execution);

        DefaultReconciler reconciler =
            new DefaultReconciler(
                stateManager,
                leaseStore,
                taskRegistry,
                tracked
            );

        try {
            Thread.sleep(50);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        ReconciliationResult result =
            reconciler.reconcile();

        assertEquals(
            1,
            result.recreatedTasks()
        );

        assertTrue(
            taskRegistry.hasPendingTask(execId)
        );
    }

    @Test
    void shouldRemoveStaleTaskForTerminalExecution() {

        InMemoryStateManager stateManager =
            new InMemoryStateManager();

        InMemoryLeaseStore leaseStore =
            new InMemoryLeaseStore();

        TaskRegistry taskRegistry =
            new TaskRegistry();

        ExecutionId execId =
            new ExecutionId("exec-recon-002");

        Execution execution =
            new Execution(
                execId,
                new EntityId("graph-1"),
                new Version("1.0.0"),
                "fp-001",
                ExecutionStatus.COMPLETED,
                Instant.now(),
                Instant.now(),
                Instant.now()
            );

        ExecutionState state =
            new ExecutionState(
                execId,
                ExecutionStatus.COMPLETED,
                new EntityId("end"),
                Map.of("result", 42),
                java.util.Set.of(
                    new EntityId("start"),
                    new EntityId("calc"),
                    new EntityId("end")
                ),
                java.util.Set.of(),
                5
            );

        stateManager.create(state);

        ExecutionTask staleTask =
            new ExecutionTask(
                "task-stale-001",
                execId,
                new EntityId("calc"),
                new Version("1.0.0"),
                "fp-001",
                1,
                Instant.now()
            );

        taskRegistry.register(staleTask);

        assertTrue(
            taskRegistry.hasPendingTask(execId)
        );

        List<Execution> tracked =
            new ArrayList<>();

        tracked.add(execution);

        DefaultReconciler reconciler =
            new DefaultReconciler(
                stateManager,
                leaseStore,
                taskRegistry,
                tracked
            );

        ReconciliationResult result =
            reconciler.reconcile();

        assertEquals(
            1,
            result.removedTasks()
        );

        assertFalse(
            taskRegistry.hasPendingTask(execId)
        );
    }

    @Test
    void shouldRemoveStaleTaskForWrongNode() {

        InMemoryStateManager stateManager =
            new InMemoryStateManager();

        InMemoryLeaseStore leaseStore =
            new InMemoryLeaseStore();

        TaskRegistry taskRegistry =
            new TaskRegistry();

        ExecutionId execId =
            new ExecutionId("exec-recon-003");

        WorkerId workerA =
            new WorkerId("worker-A");

        Execution execution =
            new Execution(
                execId,
                new EntityId("graph-1"),
                new Version("1.0.0"),
                "fp-001",
                ExecutionStatus.READY,
                Instant.now(),
                null,
                null
            );

        ExecutionState state =
            new ExecutionState(
                execId,
                ExecutionStatus.RUNNING,
                new EntityId("end"),
                Map.of("result", 42),
                java.util.Set.of(
                    new EntityId("start"),
                    new EntityId("calc")
                ),
                java.util.Set.of(),
                4
            );

        stateManager.create(state);

        leaseStore.acquire(
            execId,
            workerA,
            30_000
        );

        ExecutionTask staleTask =
            new ExecutionTask(
                "task-stale-002",
                execId,
                new EntityId("start"),
                new Version("1.0.0"),
                "fp-001",
                1,
                Instant.now()
            );

        taskRegistry.register(staleTask);

        List<Execution> tracked =
            new ArrayList<>();

        tracked.add(execution);

        DefaultReconciler reconciler =
            new DefaultReconciler(
                stateManager,
                leaseStore,
                taskRegistry,
                tracked
            );

        ReconciliationResult result =
            reconciler.reconcile();

        assertEquals(
            1,
            result.staleTasks()
        );
    }
}
