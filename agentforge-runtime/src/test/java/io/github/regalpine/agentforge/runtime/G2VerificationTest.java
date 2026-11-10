package io.github.regalpine.agentforge.runtime;

import io.github.regalpine.agentforge.event.ExecutionState;
import io.github.regalpine.agentforge.event.ExecutionStatus;
import io.github.regalpine.agentforge.event.ExecutionId;
import io.github.regalpine.agentforge.event.ExecutionEvent;

import io.github.regalpine.agentforge.kernel.EntityId;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * G2 Verification Tests — validates:
 * <ul>
 *   <li>G2-1: Terminal state correctness (all state transitions)</li>
 *   <li>G2-2: CAS contention (concurrent commits, one wins)</li>
 *   <li>G2-3: Event sequence monotonicity (execution-level)</li>
 *   <li>G2-4: Lease atomicity (covered in distributed module)</li>
 *   <li>G2-5: Atomic commit (state + event together)</li>
 *   <li>G2-6: Idempotency (covered in WorkerTaskExecutor tests)</li>
 * </ul>
 */
class G2VerificationTest {

    // ── G2-2: CAS Contention ──

    @Test
    void casContention_onlyOneCommitSucceeds() throws Exception {
        InMemoryStateManager stateManager =
            new InMemoryStateManager();

        ExecutionId id = new ExecutionId("cas-contention");
        ExecutionState initial = new ExecutionState(
            id, ExecutionStatus.READY, new EntityId("n1"),
            Map.of(), Set.of(), Set.of(), 0
        );
        stateManager.create(initial);

        int threadCount = 10;
        ExecutorService executor =
            Executors.newFixedThreadPool(threadCount);
        CountDownLatch startLatch = new CountDownLatch(1);
        AtomicInteger successCount = new AtomicInteger(0);

        for (int i = 0; i < threadCount; i++) {
            final int idx = i;
            executor.submit(() -> {
                try {
                    startLatch.await();
                    ExecutionState newState = new ExecutionState(
                        id, ExecutionStatus.RUNNING,
                        new EntityId("n" + idx),
                        Map.of("thread", idx),
                        Set.of(), Set.of(), 1
                    );
                    boolean ok = stateManager.compareAndSet(
                        id, 0, newState
                    );
                    if (ok) {
                        successCount.incrementAndGet();
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            });
        }

        startLatch.countDown();
        executor.shutdown();
        assertTrue(
            executor.awaitTermination(5, TimeUnit.SECONDS)
        );

        // Exactly one thread should succeed
        assertEquals(1, successCount.get());

        // State should be at version 1
        ExecutionState finalState =
            stateManager.get(id).orElseThrow();
        assertEquals(1L, finalState.version());
    }

    // ── G2-2: CAS rejects wrong version ──

    @Test
    void casRejects_wrongVersion() {
        InMemoryStateManager stateManager =
            new InMemoryStateManager();

        ExecutionId id = new ExecutionId("cas-wrong-ver");
        ExecutionState initial = new ExecutionState(
            id, ExecutionStatus.READY, null,
            Map.of(), Set.of(), Set.of(), 0
        );
        stateManager.create(initial);

        ExecutionState newState = new ExecutionState(
            id, ExecutionStatus.RUNNING, null,
            Map.of(), Set.of(), Set.of(), 1
        );

        // Wrong expected version (99 instead of 0)
        boolean ok = stateManager.compareAndSet(
            id, 99, newState
        );
        assertFalse(ok);

        // State should still be at version 0
        ExecutionState state =
            stateManager.get(id).orElseThrow();
        assertEquals(0L, state.version());
        assertEquals(ExecutionStatus.READY, state.status());
    }

    // ── G2-2: CAS rejects non-existent execution ──

    @Test
    void casRejects_nonExistentExecution() {
        InMemoryStateManager stateManager =
            new InMemoryStateManager();

        ExecutionId id = new ExecutionId("non-existent");
        ExecutionState newState = new ExecutionState(
            id, ExecutionStatus.RUNNING, null,
            Map.of(), Set.of(), Set.of(), 1
        );

        boolean ok = stateManager.compareAndSet(
            id, 0, newState
        );
        assertFalse(ok);
    }

    // ── G2-3: Event Sequence Monotonicity ──

    @Test
    void inMemoryCommitPort_eventSequencesMonotonic() {
        CollectingEventPublisher publisher =
            new CollectingEventPublisher();
        InMemoryCommitPort commitPort =
            new InMemoryCommitPort(publisher);

        ExecutionId id = new ExecutionId("seq-test");
        ExecutionState initial = new ExecutionState(
            id, ExecutionStatus.READY, new EntityId("n0"),
            Map.of(), Set.of(), Set.of(), 0, 0
        );
        commitPort.create(initial);

        // Simulate 5 node executions, each producing 2 events
        long nextEventSeq = 0;
        for (int i = 0; i < 5; i++) {
            // NODE_STARTED event (published directly)
            publisher.publish(new ExecutionEvent(
                id, nextEventSeq, Instant.now(),
                "NODE_STARTED",
                Map.of("nodeId", "n" + i)
            ));
            nextEventSeq++;

            // Result event (committed atomically via CommitPort)
            ExecutionState newState = new ExecutionState(
                id, ExecutionStatus.RUNNING,
                new EntityId("n" + (i + 1)),
                Map.of(), Set.of(new EntityId("n" + i)),
                Set.of(), i + 1, nextEventSeq
            );
            ExecutionEvent resultEvent = new ExecutionEvent(
                id, nextEventSeq, Instant.now(),
                "NODE_COMPLETED",
                Map.of("nodeId", "n" + i)
            );
            nextEventSeq++;

            boolean ok = commitPort.commit(
                new CommitPort.CommitCommand(
                    newState, resultEvent, i
                )
            );
            assertTrue(ok, "Commit " + i + " should succeed");
        }

        // Verify all events have strictly increasing sequences
        var events = publisher.events();
        assertEquals(10, events.size());
        for (int i = 1; i < events.size(); i++) {
            assertTrue(
                events.get(i).sequence()
                    > events.get(i - 1).sequence(),
                "Event " + i + " seq ("
                    + events.get(i).sequence()
                    + ") should be > event " + (i - 1)
                    + " seq ("
                    + events.get(i - 1).sequence() + ")"
            );
        }
    }

    // ── G2-5: Atomic Commit — state + event together ──

    @Test
    void atomicCommit_stateAndEventCommittedTogether() {
        CollectingEventPublisher publisher =
            new CollectingEventPublisher();
        InMemoryCommitPort commitPort =
            new InMemoryCommitPort(publisher);

        ExecutionId id = new ExecutionId("atomic-test");
        ExecutionState initial = new ExecutionState(
            id, ExecutionStatus.READY, new EntityId("n0"),
            Map.of(), Set.of(), Set.of(), 0, 0
        );
        commitPort.create(initial);

        ExecutionState newState = new ExecutionState(
            id, ExecutionStatus.RUNNING,
            new EntityId("n1"),
            Map.of("key", "value"),
            Set.of(new EntityId("n0")),
            Set.of(), 1, 2
        );
        ExecutionEvent event = new ExecutionEvent(
            id, 0, Instant.now(),
            "NODE_COMPLETED",
            Map.of("nodeId", "n0")
        );

        boolean ok = commitPort.commit(
            new CommitPort.CommitCommand(
                newState, event, 0
            )
        );
        assertTrue(ok);

        // State should be updated
        ExecutionState committed =
            commitPort.get(id).orElseThrow();
        assertEquals(1L, committed.version());
        assertEquals(
            ExecutionStatus.RUNNING, committed.status()
        );
        assertEquals(
            "value", committed.data().get("key")
        );
        assertTrue(
            committed.completedNodes().contains(
                new EntityId("n0")
            )
        );
        assertEquals(2L, committed.nextEventSequence());

        // Event should be published
        assertEquals(1, publisher.events().size());
        assertEquals(
            "NODE_COMPLETED",
            publisher.events().get(0).type()
        );
    }

    // ── G2-5: Atomic Commit — version conflict prevents both ──

    @Test
    void atomicCommit_versionConflict_noEventPublished() {
        CollectingEventPublisher publisher =
            new CollectingEventPublisher();
        InMemoryCommitPort commitPort =
            new InMemoryCommitPort(publisher);

        ExecutionId id = new ExecutionId("atomic-conflict");
        ExecutionState initial = new ExecutionState(
            id, ExecutionStatus.READY, null,
            Map.of(), Set.of(), Set.of(), 0, 0
        );
        commitPort.create(initial);

        // First commit succeeds
        ExecutionState s1 = new ExecutionState(
            id, ExecutionStatus.RUNNING,
            new EntityId("n1"),
            Map.of(), Set.of(), Set.of(), 1, 1
        );
        ExecutionEvent e1 = new ExecutionEvent(
            id, 0, Instant.now(),
            "NODE_STARTED", Map.of()
        );
        assertTrue(commitPort.commit(
            new CommitPort.CommitCommand(s1, e1, 0)
        ));

        // Second commit with stale version fails
        ExecutionState s2 = new ExecutionState(
            id, ExecutionStatus.COMPLETED, null,
            Map.of(), Set.of(), Set.of(), 2, 2
        );
        ExecutionEvent e2 = new ExecutionEvent(
            id, 1, Instant.now(),
            "NODE_COMPLETED", Map.of()
        );
        assertFalse(commitPort.commit(
            new CommitPort.CommitCommand(s2, e2, 0)
        ));

        // State should be at version 1 (not 2)
        ExecutionState state =
            commitPort.get(id).orElseThrow();
        assertEquals(1L, state.version());
        assertEquals(
            ExecutionStatus.RUNNING, state.status()
        );

        // Only first event should be published
        assertEquals(1, publisher.events().size());
    }

    // ── G2-1: State Machine — all terminal states ──

    @Test
    void stateMachine_allTerminalStatesValid() {
        ExecutionStateMachine sm =
            new ExecutionStateMachine();

        // RUNNING can transition to all terminal states
        assertDoesNotThrow(() -> sm.verify(
            ExecutionStatus.RUNNING,
            ExecutionStatus.COMPLETED
        ));
        assertDoesNotThrow(() -> sm.verify(
            ExecutionStatus.RUNNING,
            ExecutionStatus.FAILED
        ));
        assertDoesNotThrow(() -> sm.verify(
            ExecutionStatus.RUNNING,
            ExecutionStatus.CANCELLED
        ));
        assertDoesNotThrow(() -> sm.verify(
            ExecutionStatus.RUNNING,
            ExecutionStatus.WAITING
        ));

        // Terminal states cannot transition further
        assertThrows(IllegalStateException.class, () -> sm.verify(
            ExecutionStatus.COMPLETED,
            ExecutionStatus.RUNNING
        ));
        assertThrows(IllegalStateException.class, () -> sm.verify(
            ExecutionStatus.FAILED,
            ExecutionStatus.RUNNING
        ));
        assertThrows(IllegalStateException.class, () -> sm.verify(
            ExecutionStatus.CANCELLED,
            ExecutionStatus.RUNNING
        ));
    }

    // ── G2-1: State Machine — READY → RUNNING → terminal ──

    @Test
    void stateMachine_fullLifecycleTransitions() {
        ExecutionStateMachine sm =
            new ExecutionStateMachine();

        // CREATED → READY → RUNNING → COMPLETED
        assertDoesNotThrow(() -> sm.verify(
            ExecutionStatus.CREATED,
            ExecutionStatus.READY
        ));
        assertDoesNotThrow(() -> sm.verify(
            ExecutionStatus.READY,
            ExecutionStatus.RUNNING
        ));
        assertDoesNotThrow(() -> sm.verify(
            ExecutionStatus.RUNNING,
            ExecutionStatus.COMPLETED
        ));

        // WAITING → RUNNING (resume)
        assertDoesNotThrow(() -> sm.verify(
            ExecutionStatus.WAITING,
            ExecutionStatus.RUNNING
        ));

        // READY → CANCELLED (abort before start)
        assertDoesNotThrow(() -> sm.verify(
            ExecutionStatus.READY,
            ExecutionStatus.CANCELLED
        ));
    }

    // ── G2-3: Crash Recovery — nextEventSequence preserved ──

    @Test
    void crashRecovery_nextEventSequencePreserved() {
        InMemoryStateManager stateManager =
            new InMemoryStateManager();

        ExecutionId id = new ExecutionId("crash-recovery");

        // Simulate state after processing 3 nodes
        // (nextEventSequence = 6 because each node uses 2 seqs)
        ExecutionState state = new ExecutionState(
            id, ExecutionStatus.RUNNING,
            new EntityId("n3"),
            Map.of("step1", "a", "step2", "b", "step3", "c"),
            Set.of(
                new EntityId("n0"),
                new EntityId("n1"),
                new EntityId("n2")
            ),
            Set.of(),
            3,
            6  // nextEventSequence after 3 nodes
        );
        stateManager.create(state);

        // Recover: read state and verify
        ExecutionState recovered =
            stateManager.get(id).orElseThrow();

        assertEquals(6L, recovered.nextEventSequence());
        assertEquals(3, recovered.completedNodes().size());
        assertEquals(
            ExecutionStatus.RUNNING, recovered.status()
        );
        assertEquals("c", recovered.data().get("step3"));

        // Next event should use sequence 6
        long nextSeq = recovered.nextEventSequence();
        assertEquals(6L, nextSeq);
    }

    // ── G2-1: ExecutionState backward-compatible constructor ──

    @Test
    void executionState_backwardCompatibleConstructor() {
        ExecutionId id = new ExecutionId("compat");

        // 7-param constructor (backward compatible)
        ExecutionState state7 = new ExecutionState(
            id, ExecutionStatus.READY, new EntityId("n0"),
            Map.of("key", "val"),
            Set.of(new EntityId("n0")),
            Set.of(), 0
        );

        assertEquals(0L, state7.nextEventSequence());
        assertEquals(
            ExecutionStatus.READY, state7.status()
        );

        // 8-param constructor
        ExecutionState state8 = new ExecutionState(
            id, ExecutionStatus.RUNNING, new EntityId("n1"),
            Map.of(), Set.of(), Set.of(), 1, 4
        );

        assertEquals(4L, state8.nextEventSequence());
        assertEquals(
            ExecutionStatus.RUNNING, state8.status()
        );

        // withNextEventSequence
        ExecutionState updated =
            state8.withNextEventSequence(10);
        assertEquals(10L, updated.nextEventSequence());
        assertEquals(1L, updated.version());
    }
}
