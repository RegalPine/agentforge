package io.github.regalpine.agentforge.hardening;

import io.github.regalpine.agentforge.capability.CapabilityDefinition;
import io.github.regalpine.agentforge.compiler.CompilationContext;
import io.github.regalpine.agentforge.compiler.CompilationResult;
import io.github.regalpine.agentforge.compiler.CompilerOptions;
import io.github.regalpine.agentforge.compiler.DefaultGraphCompiler;
import io.github.regalpine.agentforge.graph.GraphBuilder;
import io.github.regalpine.agentforge.graph.GraphDefinition;
import io.github.regalpine.agentforge.kernel.EntityId;
import io.github.regalpine.agentforge.kernel.Reference;
import io.github.regalpine.agentforge.kernel.Version;
import io.github.regalpine.agentforge.compiler.RegistrySnapshot;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * AF-HARDEN-027: Concurrency - Thread-safe compilation
 * AF-HARDEN-028: Concurrent graph building
 */
class ConcurrencyTest {

    private DefaultGraphCompiler compiler;
    private CapabilityDefinition capability;
    private RegistrySnapshot snapshot;

    @BeforeEach
    void setUp() {
        compiler = new DefaultGraphCompiler();

        capability = new CapabilityDefinition(
            new EntityId("concurrent.cap"),
            new Version("1.0.0"),
            "Concurrent Cap",
            () -> "object",
            () -> "object",
            null
        );

        Reference capRef = new Reference(
            new EntityId("concurrent.cap"),
            new Version("1.0.0")
        );

        snapshot = new RegistrySnapshot(
            Map.of(),
            Map.of(capRef, capability),
            Map.of()
        );
    }

    @Test
    void shouldCompileConcurrentlyWithoutRaceCondition() throws Exception {
        GraphDefinition graph = GraphBuilder
            .graph("concurrent-test", "1.0.0", "Concurrent")
            .start("start")
            .capability("cap", capability)
            .end("end")
            .connect("start", "cap")
            .connect("cap", "end")
            .build();

        int threadCount = 8;
        int iterationsPerThread = 200;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failureCount = new AtomicInteger(0);

        for (int t = 0; t < threadCount; t++) {
            executor.submit(() -> {
                try {
                    for (int i = 0; i < iterationsPerThread; i++) {
                        CompilationResult result = compiler.compile(
                            graph,
                            new CompilationContext(snapshot, CompilerOptions.defaults())
                        );
                        if (result instanceof CompilationResult.Success) {
                            successCount.incrementAndGet();
                        } else {
                            failureCount.incrementAndGet();
                        }
                    }
                } catch (Exception e) {
                    failureCount.addAndGet(iterationsPerThread);
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await(30, TimeUnit.SECONDS);
        executor.shutdown();
        executor.awaitTermination(5, TimeUnit.SECONDS);

        assertEquals(threadCount * iterationsPerThread, successCount.get(),
            "All concurrent compilations should succeed");
        assertEquals(0, failureCount.get(),
            "No failures should occur during concurrent compilation");
    }

    @Test
    void shouldBuildGraphsConcurrently() throws Exception {
        int threadCount = 4;
        int graphsPerThread = 500;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);
        AtomicInteger successCount = new AtomicInteger(0);

        for (int t = 0; t < threadCount; t++) {
            final int threadId = t;
            executor.submit(() -> {
                try {
                    for (int i = 0; i < graphsPerThread; i++) {
                        GraphDefinition graph = GraphBuilder
                            .graph("concurrent-build-" + threadId + "-" + i, "1.0.0", "Graph")
                            .start("start")
                            .end("end")
                            .connect("start", "end")
                            .build();

                        assertNotNull(graph);
                        assertEquals(2, graph.nodes().size());
                        successCount.incrementAndGet();
                    }
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await(15, TimeUnit.SECONDS);
        executor.shutdown();
        executor.awaitTermination(5, TimeUnit.SECONDS);

        assertEquals(threadCount * graphsPerThread, successCount.get());
    }
}
