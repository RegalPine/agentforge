package io.github.regalpine.agentforge.benchmark;

import io.github.regalpine.agentforge.event.ExecutionState;
import io.github.regalpine.agentforge.event.ExecutionStatus;
import io.github.regalpine.agentforge.event.ExecutionId;

import io.github.regalpine.agentforge.capability.CapabilityDefinition;
import io.github.regalpine.agentforge.compiler.CompilationContext;
import io.github.regalpine.agentforge.compiler.CompilationResult;
import io.github.regalpine.agentforge.compiler.CompilerOptions;
import io.github.regalpine.agentforge.compiler.DefaultGraphCompiler;
import io.github.regalpine.agentforge.compiler.RegistrySnapshot;
import io.github.regalpine.agentforge.graph.GraphBuilder;
import io.github.regalpine.agentforge.graph.GraphDefinition;
import io.github.regalpine.agentforge.kernel.EntityId;
import io.github.regalpine.agentforge.kernel.Reference;
import io.github.regalpine.agentforge.kernel.Version;
import io.github.regalpine.agentforge.runtime.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Load and stress benchmarks.
 * AF-ENHANCE-07 §8-9: concurrent execution,
 * mixed load, and stress beyond capacity.
 */
@org.junit.jupiter.api.Tag("benchmark")
class LoadStressBenchmarkTest {

    private BenchmarkRunner runner;
    private CapabilityDefinition capability;
    private RegistrySnapshot snapshot;
    private DefaultGraphCompiler compiler;

    @BeforeEach
    void setUp() {
        runner = new BenchmarkRunner("load-stress");

        capability = new CapabilityDefinition(
            new EntityId("load.cap"),
            new Version("1.0.0"),
            "Load Cap",
            () -> "object",
            () -> "object",
            null
        );

        Reference capRef = new Reference(
            capability.id(), capability.version()
        );

        snapshot = new RegistrySnapshot(
            Map.of(), Map.of(capRef, capability),
            Map.of()
        );

        compiler = new DefaultGraphCompiler();
    }

    @Test
    void concurrentCompilationLoad() throws Exception {
        GraphDefinition graph = GraphBuilder
            .graph("load-graph", "1.0.0", "Load")
            .start("start")
            .capability("cap", capability)
            .end("end")
            .connect("start", "cap")
            .connect("cap", "end")
            .build();

        int threads = 8;
        int perThread = 500;
        ExecutorService exec =
            Executors.newFixedThreadPool(threads);
        CountDownLatch latch = new CountDownLatch(threads);
        AtomicInteger success = new AtomicInteger(0);

        long startNanos = System.nanoTime();

        for (int t = 0; t < threads; t++) {
            exec.submit(() -> {
                try {
                    CompilationContext ctx =
                        new CompilationContext(
                            snapshot,
                            CompilerOptions.defaults()
                        );
                    for (int i = 0; i < perThread; i++) {
                        CompilationResult r =
                            compiler.compile(graph, ctx);
                        if (r instanceof CompilationResult.Success) {
                            success.incrementAndGet();
                        }
                    }
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await(30, TimeUnit.SECONDS);
        exec.shutdown();
        exec.awaitTermination(5, TimeUnit.SECONDS);

        long totalNanos = System.nanoTime() - startNanos;
        int total = threads * perThread;
        double tps = total * 1_000_000_000.0 / totalNanos;

        System.out.printf(
            "[LOAD] Concurrent compile: %d threads × %d = %d ops in %d ms (%.0f ops/s)%n",
            threads, perThread, total,
            totalNanos / 1_000_000, tps
        );

        assertEquals(total, success.get());
        assertTrue(tps > 0);
    }

    @Test
    void concurrentStateAccessLoad() throws Exception {
        InMemoryStateManager mgr = new InMemoryStateManager();
        int stateCount = 1_000;

        for (int i = 0; i < stateCount; i++) {
            mgr.create(new ExecutionState(
                new ExecutionId("load-" + i),
                ExecutionStatus.READY,
                new EntityId("start"),
                Map.of(), Set.of(), Set.of(), 0
            ));
        }

        int threads = 4;
        int readsPerThread = 5_000;
        ExecutorService exec =
            Executors.newFixedThreadPool(threads);
        CountDownLatch latch = new CountDownLatch(threads);
        AtomicInteger totalReads = new AtomicInteger(0);

        long startNanos = System.nanoTime();

        for (int t = 0; t < threads; t++) {
            exec.submit(() -> {
                try {
                    for (int i = 0; i < readsPerThread; i++) {
                        mgr.get(new ExecutionId(
                            "load-" + (i % stateCount)
                        ));
                        totalReads.incrementAndGet();
                    }
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await(15, TimeUnit.SECONDS);
        exec.shutdown();
        exec.awaitTermination(5, TimeUnit.SECONDS);

        long totalNanos = System.nanoTime() - startNanos;
        double tps = totalReads.get() * 1_000_000_000.0
            / totalNanos;

        System.out.printf(
            "[LOAD] Concurrent state reads: %d reads in %d ms (%.0f reads/s)%n",
            totalReads.get(), totalNanos / 1_000_000, tps
        );

        assertEquals(threads * readsPerThread, totalReads.get());
    }

    @Test
    void stressBeyondCapacity() throws Exception {
        // Stress: create many states rapidly
        // to test bounds handling
        InMemoryStateManager mgr = new InMemoryStateManager();
        int threads = 8;
        int perThread = 1_000;
        ExecutorService exec =
            Executors.newFixedThreadPool(threads);
        CountDownLatch latch = new CountDownLatch(threads);
        AtomicInteger created = new AtomicInteger(0);
        AtomicInteger conflicts = new AtomicInteger(0);

        for (int t = 0; t < threads; t++) {
            final int tid = t;
            exec.submit(() -> {
                try {
                    for (int i = 0; i < perThread; i++) {
                        try {
                            mgr.create(new ExecutionState(
                                new ExecutionId(
                                    "stress-" + tid + "-" + i
                                ),
                                ExecutionStatus.READY,
                                new EntityId("start"),
                                Map.of(), Set.of(),
                                Set.of(), 0
                            ));
                            created.incrementAndGet();
                        } catch (Exception e) {
                            conflicts.incrementAndGet();
                        }
                    }
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await(30, TimeUnit.SECONDS);
        exec.shutdown();
        exec.awaitTermination(5, TimeUnit.SECONDS);

        System.out.printf(
            "[STRESS] Created: %d, Conflicts: %d%n",
            created.get(), conflicts.get()
        );

        assertEquals(threads * perThread, created.get());
        assertEquals(0, conflicts.get());
    }

    @Test
    void mixedWorkloadBenchmark() {
        InMemoryStateManager mgr = new InMemoryStateManager();

        BenchmarkResult result = runner.runParam(
            "mixed-workload",
            "LOAD",
            1_000,
            (i) -> {
                // Mix of create + read
                ExecutionId id = new ExecutionId(
                    "mixed-" + i
                );
                mgr.create(new ExecutionState(
                    id, ExecutionStatus.READY,
                    new EntityId("start"),
                    Map.of(), Set.of(), Set.of(), 0
                ));
                return mgr.get(id);
            }
        );

        System.out.println(result.summary());
        assertTrue(result.iterations() == 1_000);
    }
}
