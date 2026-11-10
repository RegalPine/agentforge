package io.github.regalpine.agentforge.persistence;

import io.github.regalpine.agentforge.event.Execution;
import io.github.regalpine.agentforge.event.ExecutionState;
import io.github.regalpine.agentforge.event.ExecutionStatus;
import io.github.regalpine.agentforge.event.ExecutionId;
import io.github.regalpine.agentforge.event.ExecutionEvent;

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

class PersistenceStoresTest {

    private InMemoryExecutionStore executionStore;
    private InMemoryExecutionStateStore stateStore;
    private InMemoryCheckpointStore checkpointStore;
    private InMemorySnapshotStore snapshotStore;
    private InMemoryEventStore eventStore;
    private InMemoryInvocationStore invocationStore;
    private InMemoryOutboxStore outboxStore;

    private final ExecutionId execId =
        new ExecutionId("exec-001");

    @BeforeEach
    void setUp() {
        executionStore = new InMemoryExecutionStore();
        stateStore = new InMemoryExecutionStateStore();
        checkpointStore = new InMemoryCheckpointStore();
        snapshotStore = new InMemorySnapshotStore();
        eventStore = new InMemoryEventStore();
        invocationStore = new InMemoryInvocationStore();
        outboxStore = new InMemoryOutboxStore();
    }

    // --- AF-PERSIST-001: Execution Persistence ---

    @Test
    void shouldCreateAndFindExecution() {
        Execution execution = new Execution(
            execId,
            new EntityId("graph-1"),
            new Version("1.0.0"),
            "sha256-abc",
            ExecutionStatus.CREATED,
            Instant.now(),
            null,
            null
        );

        executionStore.create(execution);

        var found = executionStore.find(execId);

        assertTrue(found.isPresent());
        assertEquals(
            ExecutionStatus.CREATED,
            found.get().status()
        );
    }

    @Test
    void shouldRejectDuplicateExecution() {
        Execution execution = new Execution(
            execId,
            new EntityId("graph-1"),
            new Version("1.0.0"),
            "sha256-abc",
            ExecutionStatus.CREATED,
            Instant.now(),
            null,
            null
        );

        executionStore.create(execution);

        assertThrows(
            IllegalStateException.class,
            () -> executionStore.create(execution)
        );
    }

    // --- AF-PERSIST-002/003/004: State Persistence + CAS ---

    @Test
    void shouldCreateAndFindState() {
        ExecutionState state = new ExecutionState(
            execId,
            ExecutionStatus.CREATED,
            null,
            Map.of(),
            Set.of(),
            Set.of(),
            0
        );

        stateStore.create(state);

        var found = stateStore.find(execId);

        assertTrue(found.isPresent());
        assertEquals(0, found.get().version());
    }

    @Test
    void shouldCompareAndSetState() {
        ExecutionState state = new ExecutionState(
            execId,
            ExecutionStatus.CREATED,
            null,
            Map.of(),
            Set.of(),
            Set.of(),
            0
        );

        stateStore.create(state);

        ExecutionState newState = new ExecutionState(
            execId,
            ExecutionStatus.RUNNING,
            new EntityId("node-1"),
            Map.of("key", "value"),
            Set.of(new EntityId("start")),
            Set.of(),
            1
        );

        boolean result = stateStore.compareAndSet(
            execId, 0, newState
        );

        assertTrue(result);

        var found = stateStore.find(execId);
        assertEquals(1, found.get().version());
        assertEquals(
            ExecutionStatus.RUNNING,
            found.get().status()
        );
    }

    @Test
    void shouldRejectCASWithWrongVersion() {
        ExecutionState state = new ExecutionState(
            execId,
            ExecutionStatus.CREATED,
            null,
            Map.of(),
            Set.of(),
            Set.of(),
            0
        );

        stateStore.create(state);

        ExecutionState newState = new ExecutionState(
            execId,
            ExecutionStatus.RUNNING,
            null,
            Map.of(),
            Set.of(),
            Set.of(),
            1
        );

        boolean result = stateStore.compareAndSet(
            execId, 99, newState
        );

        assertFalse(result);

        var found = stateStore.find(execId);
        assertEquals(0, found.get().version());
    }

    // --- AF-PERSIST-005/006: Checkpoint ---

    @Test
    void shouldSaveAndFindLatestCheckpoint() {
        Checkpoint cp1 = new Checkpoint(
            "cp-1", execId, 1, 1,
            new EntityId("node-1"),
            io.github.regalpine.agentforge.event.ExecutionStatus.RUNNING,
            Map.of("a", 1),
            null,
            Instant.now()
        );

        Checkpoint cp2 = new Checkpoint(
            "cp-2", execId, 3, 3,
            new EntityId("node-3"),
            io.github.regalpine.agentforge.event.ExecutionStatus.RUNNING,
            Map.of("a", 3),
            null,
            Instant.now()
        );

        checkpointStore.save(cp1);
        checkpointStore.save(cp2);

        var latest = checkpointStore.latest(execId);

        assertTrue(latest.isPresent());
        assertEquals(3, latest.get().stateVersion());
        assertEquals("cp-2", latest.get().checkpointId());
    }

    // --- AF-PERSIST-007/008/009: Event ---

    @Test
    void shouldAppendAndListEvents() {
        ExecutionEvent e1 = new ExecutionEvent(
            execId, 1, Instant.now(),
            "NODE_STARTED", Map.of()
        );

        ExecutionEvent e2 = new ExecutionEvent(
            execId, 2, Instant.now(),
            "NODE_COMPLETED", Map.of()
        );

        eventStore.append(e1);
        eventStore.append(e2);

        var all = eventStore.list(execId);
        assertEquals(2, all.size());

        var after = eventStore.listAfter(execId, 1);
        assertEquals(1, after.size());
        assertEquals(2, after.get(0).sequence());
    }

    @Test
    void shouldRejectNonMonotonicSequence() {
        ExecutionEvent e1 = new ExecutionEvent(
            execId, 5, Instant.now(),
            "NODE_STARTED", Map.of()
        );

        ExecutionEvent e2 = new ExecutionEvent(
            execId, 3, Instant.now(),
            "NODE_COMPLETED", Map.of()
        );

        eventStore.append(e1);

        assertThrows(
            IllegalStateException.class,
            () -> eventStore.append(e2)
        );
    }

    // --- AF-PERSIST-010: Snapshot Recovery ---

    @Test
    void shouldSaveAndFindLatestSnapshot() {
        Snapshot s1 = new Snapshot(
            "snap-1", execId, 5, 3,
            new EntityId("node-3"),
            io.github.regalpine.agentforge.event.ExecutionStatus.RUNNING,
            Map.of("data", "v3"),
            null,
            Instant.now()
        );

        Snapshot s2 = new Snapshot(
            "snap-2", execId, 10, 7,
            new EntityId("node-7"),
            io.github.regalpine.agentforge.event.ExecutionStatus.RUNNING,
            Map.of("data", "v7"),
            null,
            Instant.now()
        );

        snapshotStore.save(s1);
        snapshotStore.save(s2);

        var latest = snapshotStore.latest(execId);

        assertTrue(latest.isPresent());
        assertEquals(7, latest.get().stateVersion());
        assertEquals(10, latest.get().eventSequence());
    }

    // --- AF-PERSIST-011/012: Invocation + Idempotency ---

    @Test
    void shouldPersistInvocationAndIdempotency() {
        StoredInvocation inv = new StoredInvocation(
            "inv-001", execId,
            new EntityId("calculator.evaluate"),
            new Version("1.0.0"),
            "idem-key-001",
            "PENDING",
            Instant.now(),
            null,
            null
        );

        invocationStore.create(inv);

        var byId = invocationStore.find("inv-001");
        assertTrue(byId.isPresent());

        var byKey = invocationStore.findByIdempotencyKey(
            "idem-key-001"
        );
        assertTrue(byKey.isPresent());
        assertEquals("inv-001", byKey.get().invocationId());
    }

    // --- AF-PERSIST-013/014: Outbox ---

    @Test
    void shouldPersistOutboxAndMarkPublished() {
        OutboxMessage msg = new OutboxMessage(
            "msg-001", "exec-001",
            "NODE_COMPLETED", "{}",
            Instant.now(), false
        );

        outboxStore.append(msg);

        var unpublished = outboxStore.unpublished(10);
        assertEquals(1, unpublished.size());
        assertFalse(unpublished.get(0).published());

        outboxStore.markPublished("msg-001");

        var after = outboxStore.unpublished(10);
        assertEquals(0, after.size());
    }

    // --- AF-PERSIST-015: DurableEventPublisher ---

    @Test
    void shouldPublishToEventStoreAndOutbox() {
        DurableEventPublisher publisher =
            new DurableEventPublisher(
                eventStore, outboxStore
            );

        ExecutionEvent event = new ExecutionEvent(
            execId, 1, Instant.now(),
            "NODE_STARTED",
            Map.of("node", "start")
        );

        publisher.publish(event);

        var events = eventStore.list(execId);
        assertEquals(1, events.size());

        var outbox = outboxStore.unpublished(10);
        assertEquals(1, outbox.size());
        assertEquals(
            "exec-001-1",
            outbox.get(0).messageId()
        );
    }

    // --- PersistentStateManager ---

    @Test
    void shouldDelegateToStateStore() {
        PersistentStateManager manager =
            new PersistentStateManager(stateStore);

        ExecutionState state = new ExecutionState(
            execId,
            ExecutionStatus.CREATED,
            null,
            Map.of(),
            Set.of(),
            Set.of(),
            0
        );

        manager.create(state);

        var found = manager.get(execId);
        assertTrue(found.isPresent());

        ExecutionState updated = new ExecutionState(
            execId,
            ExecutionStatus.RUNNING,
            new EntityId("node-1"),
            Map.of(),
            Set.of(),
            Set.of(),
            1
        );

        assertTrue(
            manager.compareAndSet(execId, 0, updated)
        );

        assertFalse(
            manager.compareAndSet(execId, 0, updated)
        );
    }
}
