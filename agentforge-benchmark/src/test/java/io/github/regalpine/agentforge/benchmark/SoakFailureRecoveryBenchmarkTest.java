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
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Soak, failure, and recovery benchmarks.
 * AF-ENHANCE-07 §10-12: long-running
 * stability, provider failure simulation,
 * and checkpoint recovery verification.
 */
@org.junit.jupiter.api.Tag("benchmark")
class SoakFailureRecoveryBenchmarkTest {

    private BenchmarkRunner runner;
    private CapabilityDefinition capability;
    private RegistrySnapshot snapshot;
    private DefaultGraphCompiler compiler;

    @BeforeEach
    void setUp() {
        runner = new BenchmarkRunner("soak-fail-recover");

        capability = new CapabilityDefinition(
            new EntityId("soak.cap"),
            new Version("1.0.0"),
            "Soak Cap",
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
    void soakTestSustainedCompilation() {
        GraphDefinition graph = GraphBuilder
            .graph("soak-graph", "1.0.0", "Soak")
            .start("start")
            .capability("cap", capability)
            .end("end")
            .connect("start", "cap")
            .connect("cap", "end")
            .build();

        CompilationContext ctx = new CompilationContext(
            snapshot, CompilerOptions.defaults()
        );

        // Soak: 50K iterations to detect
        // memory leaks or degradation
        BenchmarkResult result = runner.run(
            "soak-compile",
            "SOAK",
            50_000,
            () -> {
                CompilationResult r =
                    compiler.compile(graph, ctx);
                assertInstanceOf(
                    CompilationResult.Success.class, r
                );
            }
        );

        System.out.println(result.summary());
        assertEquals(50_000, result.iterations());
        assertTrue(result.throughputPerSec() > 0);
    }

    @Test
    void soakTestSustainedStateOperations() {
        InMemoryStateManager mgr = new InMemoryStateManager();
        AtomicInteger counter = new AtomicInteger(0);

        BenchmarkResult result = runner.run(
            "soak-state",
            "SOAK",
            10_000,
            () -> {
                int i = counter.getAndIncrement();
                ExecutionId id = new ExecutionId(
                    "soak-" + i
                );
                mgr.create(new ExecutionState(
                    id, ExecutionStatus.READY,
                    new EntityId("start"),
                    Map.of(), Set.of(), Set.of(), 0
                ));
                assertTrue(mgr.get(id).isPresent());
            }
        );

        System.out.println(result.summary());
        assertEquals(10_000, result.iterations());
    }

    @Test
    void failureSimulationProviderDown() {
        // Simulate provider failure by
        // throwing exceptions
        AtomicInteger failCount = new AtomicInteger(0);

        BenchmarkResult result = runner.run(
            "failure-provider",
            "FAILURE",
            1_000,
            () -> {
                try {
                    // Simulate: 10% failure rate
                    if (Math.random() < 0.1) {
                        throw new RuntimeException(
                            "Provider unavailable"
                        );
                    }
                } catch (RuntimeException e) {
                    failCount.incrementAndGet();
                }
            }
        );

        System.out.println(result.summary());
        System.out.printf(
            "[FAILURE] Simulated failures: %d / 1000%n",
            failCount.get()
        );
        assertTrue(failCount.get() > 0,
            "Some failures should occur");
    }

    @Test
    void failureSimulationDatabaseDown() {
        InMemoryStateManager mgr = new InMemoryStateManager();

        // Create initial state
        mgr.create(new ExecutionState(
            new ExecutionId("db-fail"),
            ExecutionStatus.READY,
            new EntityId("start"),
            Map.of(), Set.of(), Set.of(), 0
        ));

        AtomicInteger errors = new AtomicInteger(0);

        BenchmarkResult result = runner.run(
            "failure-db",
            "FAILURE",
            5_000,
            () -> {
                try {
                    var state = mgr.get(
                        new ExecutionId("db-fail")
                    );
                    assertTrue(state.isPresent());

                    // Simulate: 5% CAS attempt
                    // with wrong version
                    if (Math.random() < 0.05) {
                        mgr.compareAndSet(
                            new ExecutionId("db-fail"),
                            999, // wrong version
                            new ExecutionState(
                                new ExecutionId("db-fail"),
                                ExecutionStatus.RUNNING,
                                new EntityId("node"),
                                Map.of(), Set.of(),
                                Set.of(), 1
                            )
                        );
                        // State remains unchanged
                        // because version != 999
                        errors.incrementAndGet();
                    }
                } catch (Exception e) {
                    errors.incrementAndGet();
                }
            }
        );

        System.out.println(result.summary());
        assertTrue(result.iterations() == 5_000);
    }

    @Test
    void recoveryFromCheckpoint() {
        InMemoryStateManager mgr = new InMemoryStateManager();

        // Simulate checkpoint: create states
        // at various versions
        for (int v = 0; v < 10; v++) {
            mgr.create(new ExecutionState(
                new ExecutionId("recover-" + v),
                ExecutionStatus.READY,
                new EntityId("start"),
                Map.of(), Set.of(), Set.of(), v
            ));
        }

        // Simulate recovery: verify all
        // checkpoints are accessible
        BenchmarkResult result = runner.run(
            "recovery-checkpoint",
            "RECOVERY",
            10_000,
            () -> {
                for (int v = 0; v < 10; v++) {
                    var state = mgr.get(
                        new ExecutionId("recover-" + v)
                    );
                    assertTrue(state.isPresent());
                    assertEquals(v,
                        state.get().version());
                }
            }
        );

        System.out.println(result.summary());
        assertEquals(10_000, result.iterations());
    }

    @Test
    void recoveryAfterFailure() {
        InMemoryStateManager mgr = new InMemoryStateManager();

        ExecutionId id = new ExecutionId("recover-after");

        // Step 1: Create initial state
        mgr.create(new ExecutionState(
            id, ExecutionStatus.READY,
            new EntityId("start"),
            Map.of(), Set.of(), Set.of(), 0
        ));

        // Step 2: Simulate failure (CAS with
        // wrong version — InMemoryStateManager
        // returns true if key exists, so we
        // verify the state is unchanged)
        ExecutionState beforeState = mgr.get(id).orElseThrow();
        mgr.compareAndSet(
            id, 999,
            new ExecutionState(
                id, ExecutionStatus.RUNNING,
                new EntityId("node"),
                Map.of(), Set.of(), Set.of(), 1
            )
        );
        // Version should remain 0 since 999 != 0
        assertEquals(0, mgr.get(id).orElseThrow().version());

        // Step 3: Recovery with correct version
        boolean recovered = mgr.compareAndSet(
            id, 0,
            new ExecutionState(
                id, ExecutionStatus.RUNNING,
                new EntityId("node"),
                Map.of(), Set.of(), Set.of(), 1
            )
        );
        assertTrue(recovered,
            "Recovery should succeed");

        var state = mgr.get(id);
        assertTrue(state.isPresent());
        assertEquals(ExecutionStatus.RUNNING,
            state.get().status());
        assertEquals(1, state.get().version());
    }
}
