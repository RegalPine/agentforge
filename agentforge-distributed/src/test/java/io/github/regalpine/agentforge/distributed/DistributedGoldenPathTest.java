package io.github.regalpine.agentforge.distributed;

import io.github.regalpine.agentforge.event.Execution;
import io.github.regalpine.agentforge.event.ExecutionState;
import io.github.regalpine.agentforge.event.ExecutionEvent;
import io.github.regalpine.agentforge.event.ExecutionId;

import io.github.regalpine.agentforge.capability.*;
import io.github.regalpine.agentforge.compiler.CompilationContext;
import io.github.regalpine.agentforge.compiler.CompilationResult;
import io.github.regalpine.agentforge.compiler.DefaultGraphCompiler;
import io.github.regalpine.agentforge.compiler.RegistrySnapshot;
import io.github.regalpine.agentforge.distributed.fencing.DefaultFencingValidator;
import io.github.regalpine.agentforge.distributed.memory.InMemoryLeaseStore;
import io.github.regalpine.agentforge.distributed.memory.InMemoryTaskQueue;
import io.github.regalpine.agentforge.distributed.worker.WorkerId;
import io.github.regalpine.agentforge.distributed.worker.WorkerTaskExecutor;
import io.github.regalpine.agentforge.graph.GraphBuilder;
import io.github.regalpine.agentforge.graph.GraphDefinition;
import io.github.regalpine.agentforge.graph.NodeType;
import io.github.regalpine.agentforge.graph.ir.ExecutionPlan;
import io.github.regalpine.agentforge.kernel.EntityId;
import io.github.regalpine.agentforge.kernel.Reference;
import io.github.regalpine.agentforge.kernel.Schema;
import io.github.regalpine.agentforge.kernel.Version;
import io.github.regalpine.agentforge.runtime.*;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class DistributedGoldenPathTest {

    private static CapabilityProvider mockProvider(
        CapabilityResult result
    ) {
        return new CapabilityProvider() {
            @Override
            public ProviderDescriptor descriptor() {
                return new ProviderDescriptor(
                    "mock", new Version("1.0.0"),
                    Set.of(), Map.of()
                );
            }

            @Override
            public CapabilityResult invoke(
                CapabilityInvocation invocation
            ) {
                return result;
            }
        };
    }

    @Test
    void shouldExecuteStartCapabilityEndDistributed() {

        Schema schema = () -> "object";

        CapabilityDefinition capability =
            new CapabilityDefinition(
                new EntityId("calculator.evaluate"),
                new Version("1.0.0"),
                "Calculator",
                schema,
                schema,
                null
            );

        GraphDefinition graph =
            GraphBuilder
                .graph("dist-demo", "1.0.0", "Dist Demo")
                .start("start")
                .capability("calc", capability)
                .end("end")
                .connect("start", "calc")
                .connect("calc", "end")
                .build();

        DefaultGraphCompiler compiler =
            new DefaultGraphCompiler();

        Reference capRef = new Reference(
            new EntityId("calculator.evaluate"),
            new Version("1.0.0")
        );

        CompilationResult result =
            compiler.compile(
                graph,
                new CompilationContext(
                    new RegistrySnapshot(
                        Map.of(),
                        Map.of(capRef, capability),
                        Map.of()
                    ),
                    null
                )
            );

        assertInstanceOf(
            CompilationResult.Success.class,
            result
        );

        ExecutionPlan plan =
            ((CompilationResult.Success) result)
                .executionPlan();

        InMemoryExecutionPlanRegistry planRegistry =
            new InMemoryExecutionPlanRegistry();

        planRegistry.register(plan);

        InMemoryStateManager stateManager =
            new InMemoryStateManager();

        CollectingEventPublisher eventPublisher =
            new CollectingEventPublisher();

        NodeExecutorRegistry executorRegistry =
            new NodeExecutorRegistry();

        executorRegistry.register(
            NodeType.START,
            new StartNodeExecutor()
        );

        executorRegistry.register(
            NodeType.END,
            new EndNodeExecutor()
        );

        CapabilityExecutor capabilityExecutor =
            new CapabilityExecutor(
                new SimpleCapabilityProviderResolver(
                    Map.of(
                        capRef,
                        mockProvider(
                            new CapabilitySuccess(
                                Map.of("result", 42)
                            )
                        )
                    )
                )
            );

        executorRegistry.register(
            NodeType.CAPABILITY,
            new CapabilityNodeExecutor(capabilityExecutor)
        );

        ExecutionManager manager =
            new ExecutionManager(planRegistry, stateManager);

        ExecutionHandle handle =
            manager.submit(
                new ExecutionRequest(
                    new EntityId("dist-demo"),
                    new Version("1.0.0"),
                    Map.of("message", "hello"),
                    null
                )
            );

        Execution execution =
            manager.get(handle.executionId())
                .orElseThrow();

        InMemoryLeaseStore leaseStore =
            new InMemoryLeaseStore();

        InMemoryTaskQueue taskQueue =
            new InMemoryTaskQueue();

        WorkerTaskExecutor workerExecutor =
            new WorkerTaskExecutor(
                leaseStore,
                new DefaultFencingValidator(),
                stateManager,
                planRegistry,
                executorRegistry,
                eventPublisher,
                taskQueue
            );

        WorkerId workerA =
            new WorkerId("worker-A");

        ExecutionState initialState =
            stateManager.get(handle.executionId())
                .orElseThrow();

        ExecutionTask startTask =
            ExecutionTask.create(
                execution,
                initialState.currentNode()
            );

        workerExecutor.executeTask(
            startTask,
            workerA,
            execution
        );

        ExecutionState afterStart =
            stateManager.get(handle.executionId())
                .orElseThrow();

        assertTrue(
            afterStart.completedNodes().contains(
                new EntityId("start")
            )
        );

        Optional<ExecutionTask> nextTask =
            taskQueue.poll(Duration.ZERO);

        assertTrue(nextTask.isPresent());

        assertEquals(
            "calc",
            nextTask.get().nodeId().value()
        );

        WorkerId workerB =
            new WorkerId("worker-B");

        workerExecutor.executeTask(
            nextTask.get(),
            workerB,
            execution
        );

        ExecutionState afterCalc =
            stateManager.get(handle.executionId())
                .orElseThrow();

        assertTrue(
            afterCalc.completedNodes().contains(
                new EntityId("calc")
            )
        );

        assertEquals(
            42,
            afterCalc.data().get("result")
        );

        Optional<ExecutionTask> endTask =
            taskQueue.poll(Duration.ZERO);

        assertTrue(endTask.isPresent());

        assertEquals(
            "end",
            endTask.get().nodeId().value()
        );

        WorkerId workerC =
            new WorkerId("worker-C");

        workerExecutor.executeTask(
            endTask.get(),
            workerC,
            execution
        );

        ExecutionState finalState =
            stateManager.get(handle.executionId())
                .orElseThrow();

        assertTrue(
            finalState.completedNodes().contains(
                new EntityId("end")
            )
        );

        List<ExecutionEvent> events =
            eventPublisher.events();

        assertFalse(events.isEmpty());

        List<String> eventTypes =
            events.stream()
                .map(ExecutionEvent::type)
                .toList();

        assertTrue(
            eventTypes.contains("NODE_STARTED")
        );

        assertTrue(
            eventTypes.contains("NODE_COMPLETED")
        );

        long startedCount =
            events.stream()
                .filter(
                    e -> "NODE_STARTED".equals(e.type())
                )
                .count();

        assertEquals(3, startedCount);
    }

    @Test
    void shouldRejectStaleWorkerViaFencing() {

        InMemoryLeaseStore leaseStore =
            new InMemoryLeaseStore();

        ExecutionId execId =
            new ExecutionId("exec-fencing-001");

        WorkerId workerA =
            new WorkerId("worker-A");

        WorkerId workerB =
            new WorkerId("worker-B");

        var leaseA =
            leaseStore.acquire(
                execId,
                workerA,
                30_000
            );

        assertTrue(leaseA.isPresent());

        long tokenA =
            leaseA.get().fencingToken();

        leaseStore.release(
            execId,
            workerA,
            tokenA
        );

        var leaseB =
            leaseStore.acquire(
                execId,
                workerB,
                30_000
            );

        assertTrue(leaseB.isPresent());

        long tokenB =
            leaseB.get().fencingToken();

        assertTrue(tokenB > tokenA);

        boolean renewed =
            leaseStore.renew(
                execId,
                workerA,
                tokenA,
                30_000
            );

        assertFalse(renewed);

        boolean released =
            leaseStore.release(
                execId,
                workerA,
                tokenA
            );

        assertFalse(released);
    }

    @Test
    void shouldPreventDuplicateLease() {

        InMemoryLeaseStore leaseStore =
            new InMemoryLeaseStore();

        ExecutionId execId =
            new ExecutionId("exec-dup-001");

        WorkerId workerA =
            new WorkerId("worker-A");

        WorkerId workerB =
            new WorkerId("worker-B");

        var first =
            leaseStore.acquire(
                execId,
                workerA,
                30_000
            );

        assertTrue(first.isPresent());

        var second =
            leaseStore.acquire(
                execId,
                workerB,
                30_000
            );

        assertTrue(second.isEmpty());
    }
}
