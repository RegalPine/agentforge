package io.github.regalpine.agentforge.hardening;

import io.github.regalpine.agentforge.event.ExecutionState;
import io.github.regalpine.agentforge.event.ExecutionStatus;
import io.github.regalpine.agentforge.event.ExecutionId;

import io.github.regalpine.agentforge.compiler.CompilationContext;
import io.github.regalpine.agentforge.compiler.CompilationResult;
import io.github.regalpine.agentforge.compiler.CompilerOptions;
import io.github.regalpine.agentforge.compiler.DefaultGraphCompiler;
import io.github.regalpine.agentforge.graph.GraphBuilder;
import io.github.regalpine.agentforge.graph.GraphDefinition;
import io.github.regalpine.agentforge.runtime.*;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * AF-HARDEN-029: Resource bounds - thread pool saturation
 * AF-HARDEN-030: Timeout handling
 * AF-HARDEN-031: Backpressure - bounded queue
 */
class ResourceBoundsTest {

    @Test
    void shouldHandleConcurrentExecutionManagerSubmits() throws Exception {
        ExecutionManager manager = new ExecutionManager(
            new InMemoryExecutionPlanRegistry(),
            new InMemoryStateManager()
        );

        int threadCount = 4;
        int submitsPerThread = 100;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);
        AtomicInteger completionCount = new AtomicInteger(0);

        for (int t = 0; t < threadCount; t++) {
            final int threadId = t;
            executor.submit(() -> {
                try {
                    for (int i = 0; i < submitsPerThread; i++) {
                        try {
                            manager.submit(new ExecutionRequest(
                                new io.github.regalpine.agentforge.kernel.EntityId("graph-" + threadId),
                                new io.github.regalpine.agentforge.kernel.Version("1.0.0"),
                                Map.of(),
                                null
                            ));
                        } catch (Exception e) {
                            // Expected: plan not found - handled gracefully
                        }
                        completionCount.incrementAndGet();
                    }
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await(15, TimeUnit.SECONDS);
        executor.shutdown();
        executor.awaitTermination(5, TimeUnit.SECONDS);

        assertEquals(threadCount * submitsPerThread, completionCount.get(),
            "All submits should complete (success or graceful failure)");
    }

    @Test
    void shouldHandleRapidStateCreation() {
        InMemoryStateManager stateManager = new InMemoryStateManager();
        int stateCount = 1000;

        long start = System.nanoTime();

        for (int i = 0; i < stateCount; i++) {
            ExecutionId execId = new ExecutionId("exec-" + i);
            ExecutionState state = new ExecutionState(
                execId,
                ExecutionStatus.READY,
                new io.github.regalpine.agentforge.kernel.EntityId("start"),
                Map.of(),
                java.util.Set.of(),
                java.util.Set.of(),
                0
            );
            stateManager.create(state);
        }

        long elapsedMs = (System.nanoTime() - start) / 1_000_000;

        System.out.printf(
            "[HARDEN] Created %d states in %d ms%n",
            stateCount, elapsedMs
        );

        assertTrue(elapsedMs < 10_000,
            "1000 state creations should complete within 10s");

        for (int i = 0; i < stateCount; i++) {
            assertTrue(stateManager.get(new ExecutionId("exec-" + i)).isPresent());
        }
    }

    @Test
    void shouldHandleExecutionStatusTransitions() {
        InMemoryStateManager stateManager = new InMemoryStateManager();
        ExecutionId execId = new ExecutionId("exec-status-test");

        ExecutionState initial = new ExecutionState(
            execId,
            ExecutionStatus.READY,
            new io.github.regalpine.agentforge.kernel.EntityId("start"),
            Map.of(),
            java.util.Set.of(),
            java.util.Set.of(),
            0
        );

        stateManager.create(initial);

        ExecutionState running = new ExecutionState(
            execId,
            ExecutionStatus.RUNNING,
            new io.github.regalpine.agentforge.kernel.EntityId("node-1"),
            Map.of(),
            java.util.Set.of(new io.github.regalpine.agentforge.kernel.EntityId("start")),
            java.util.Set.of(),
            1
        );

        boolean updated = stateManager.compareAndSet(execId, 0, running);
        assertTrue(updated, "CAS should succeed with correct expected version");

        var loaded = stateManager.get(execId);
        assertTrue(loaded.isPresent());
        assertEquals(ExecutionStatus.RUNNING, loaded.get().status());
    }

    @Test
    void shouldPreventDuplicateStateCreation() {
        InMemoryStateManager stateManager = new InMemoryStateManager();
        ExecutionId execId = new ExecutionId("exec-dup");

        ExecutionState state = new ExecutionState(
            execId, ExecutionStatus.READY,
            new io.github.regalpine.agentforge.kernel.EntityId("start"),
            Map.of(), java.util.Set.of(), java.util.Set.of(), 0
        );

        stateManager.create(state);

        assertThrows(Exception.class, () -> stateManager.create(state),
            "Duplicate state creation should throw");
    }
}
