package io.github.regalpine.agentforge.distributed;

import io.github.regalpine.agentforge.distributed.fencing.DefaultFencingValidator;
import io.github.regalpine.agentforge.distributed.heartbeat.WorkerHeartbeat;
import io.github.regalpine.agentforge.distributed.memory.InMemoryLeaseStore;
import io.github.regalpine.agentforge.distributed.memory.InMemoryTaskQueue;
import io.github.regalpine.agentforge.distributed.memory.InMemoryWorkerRegistry;
import io.github.regalpine.agentforge.distributed.memory.InMemoryWorkerRegistrationStore;
import io.github.regalpine.agentforge.distributed.queue.TaskQueue;
import io.github.regalpine.agentforge.distributed.scheduler.QueueDistributedScheduler;
import io.github.regalpine.agentforge.distributed.scheduler.RoundRobinWorkerSelector;
import io.github.regalpine.agentforge.distributed.scheduler.WorkerSelector;
import io.github.regalpine.agentforge.distributed.worker.*;
import io.github.regalpine.agentforge.kernel.EntityId;
import io.github.regalpine.agentforge.kernel.Version;
import io.github.regalpine.agentforge.event.ExecutionId;
import io.github.regalpine.agentforge.runtime.ExecutionTask;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class WorkerAndSchedulerTest {

    @Test
    void shouldRegisterAndHeartbeatWorker() {

        InMemoryWorkerRegistry registry =
            new InMemoryWorkerRegistry();

        WorkerId workerId =
            new WorkerId("worker-001");

        WorkerRegistration registration =
            new WorkerRegistration(
                workerId,
                WorkerStatus.ACTIVE,
                Set.of("capability", "compute"),
                Instant.now(),
                Instant.now()
            );

        registry.register(registration);

        Optional<WorkerRegistration> found =
            registry.find(workerId);

        assertTrue(found.isPresent());

        assertEquals(
            WorkerStatus.ACTIVE,
            found.get().status()
        );

        registry.heartbeat(workerId);

        found = registry.find(workerId);

        assertTrue(found.isPresent());

        registry.markDraining(workerId);

        found = registry.find(workerId);

        assertEquals(
            WorkerStatus.DRAINING,
            found.get().status()
        );

        List<WorkerRegistration> active =
            registry.activeWorkers();

        assertTrue(active.isEmpty());
    }

    @Test
    void shouldDetectDeadWorkerByHeartbeat() {

        InMemoryWorkerRegistrationStore store =
            new InMemoryWorkerRegistrationStore(
                Duration.ofMillis(50)
            );

        WorkerId workerId =
            new WorkerId("worker-dead");

        WorkerRegistration registration =
            new WorkerRegistration(
                workerId,
                WorkerStatus.ACTIVE,
                Set.of(),
                Instant.now(),
                Instant.now()
            );

        store.register(registration);

        assertTrue(store.isAlive(workerId));

        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        assertFalse(store.isAlive(workerId));
    }

    @Test
    void shouldSelectWorkerRoundRobin() {

        RoundRobinWorkerSelector selector =
            new RoundRobinWorkerSelector();

        WorkerRegistration w1 =
            new WorkerRegistration(
                new WorkerId("w1"),
                WorkerStatus.ACTIVE,
                Set.of(),
                Instant.now(),
                Instant.now()
            );

        WorkerRegistration w2 =
            new WorkerRegistration(
                new WorkerId("w2"),
                WorkerStatus.ACTIVE,
                Set.of(),
                Instant.now(),
                Instant.now()
            );

        List<WorkerRegistration> workers =
            List.of(w1, w2);

        ExecutionTask task =
            new ExecutionTask(
                "task-001",
                new ExecutionId("exec-001"),
                new EntityId("node-1"),
                new Version("1.0.0"),
                "fp-001",
                1,
                Instant.now()
            );

        WorkerRegistration first =
            selector.select(task, workers);

        WorkerRegistration second =
            selector.select(task, workers);

        assertNotEquals(
            first.workerId(),
            second.workerId()
        );
    }

    @Test
    void shouldEnqueueAndPollTask() {

        InMemoryTaskQueue queue =
            new InMemoryTaskQueue();

        ExecutionTask task =
            new ExecutionTask(
                "task-001",
                new ExecutionId("exec-001"),
                new EntityId("node-1"),
                new Version("1.0.0"),
                "fp-001",
                1,
                Instant.now()
            );

        queue.enqueue(task);

        assertEquals(1, queue.size());

        Optional<ExecutionTask> polled =
            queue.poll(Duration.ZERO);

        assertTrue(polled.isPresent());

        assertEquals(
            "task-001",
            polled.get().taskId()
        );

        assertEquals(0, queue.size());

        queue.acknowledge("task-001");

        assertTrue(
            queue.isAcknowledged("task-001")
        );
    }

    @Test
    void shouldScheduleViaQueue() {

        InMemoryTaskQueue queue =
            new InMemoryTaskQueue();

        QueueDistributedScheduler scheduler =
            new QueueDistributedScheduler(queue);

        ExecutionTask task =
            new ExecutionTask(
                "task-sched-001",
                new ExecutionId("exec-001"),
                new EntityId("node-1"),
                new Version("1.0.0"),
                "fp-001",
                1,
                Instant.now()
            );

        scheduler.schedule(task);

        assertEquals(1, queue.size());

        Optional<ExecutionTask> polled =
            queue.poll(Duration.ZERO);

        assertTrue(polled.isPresent());

        assertEquals(
            "task-sched-001",
            polled.get().taskId()
        );
    }

    @Test
    void shouldValidateFencingToken() {

        DefaultFencingValidator validator =
            new DefaultFencingValidator();

        InMemoryLeaseStore leaseStore =
            new InMemoryLeaseStore();

        ExecutionId execId =
            new ExecutionId("exec-fence-001");

        WorkerId workerId =
            new WorkerId("worker-001");

        var lease =
            leaseStore.acquire(
                execId,
                workerId,
                30_000
            );

        assertTrue(lease.isPresent());
        assertTrue(validator.isValid(lease.get()));

        leaseStore.release(
            execId,
            workerId,
            lease.get().fencingToken()
        );

        assertFalse(validator.isValid(null));
    }

    @Test
    void shouldRejectBlankWorkerId() {

        assertThrows(
            IllegalArgumentException.class,
            () -> new WorkerId("")
        );

        assertThrows(
            NullPointerException.class,
            () -> new WorkerId(null)
        );
    }
}
