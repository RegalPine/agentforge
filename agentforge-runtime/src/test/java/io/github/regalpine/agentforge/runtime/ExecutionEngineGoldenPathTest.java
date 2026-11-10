package io.github.regalpine.agentforge.runtime;

import io.github.regalpine.agentforge.event.Execution;
import io.github.regalpine.agentforge.event.ExecutionState;

import io.github.regalpine.agentforge.capability.*;
import io.github.regalpine.agentforge.compiler.CompilationResult;
import io.github.regalpine.agentforge.compiler.CompilationContext;
import io.github.regalpine.agentforge.compiler.DefaultGraphCompiler;
import io.github.regalpine.agentforge.compiler.RegistrySnapshot;
import io.github.regalpine.agentforge.event.ExecutionEvent;
import io.github.regalpine.agentforge.graph.GraphBuilder;
import io.github.regalpine.agentforge.graph.GraphDefinition;
import io.github.regalpine.agentforge.graph.ir.ExecutionPlan;
import io.github.regalpine.agentforge.kernel.EntityId;
import io.github.regalpine.agentforge.kernel.Reference;
import io.github.regalpine.agentforge.kernel.Schema;
import io.github.regalpine.agentforge.kernel.Version;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class ExecutionEngineGoldenPathTest {

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
    void shouldExecuteStartCapabilityEnd() {

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
                .graph("demo", "1.0.0", "Demo")
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

        // Setup runtime
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
            io.github.regalpine.agentforge.graph.NodeType.START,
            new StartNodeExecutor()
        );

        executorRegistry.register(
            io.github.regalpine.agentforge.graph.NodeType.END,
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
            io.github.regalpine.agentforge.graph.NodeType.CAPABILITY,
            new CapabilityNodeExecutor(capabilityExecutor)
        );

        // Create execution
        ExecutionManager manager =
            new ExecutionManager(planRegistry, stateManager);

        ExecutionHandle handle =
            manager.submit(
                new ExecutionRequest(
                    new EntityId("demo"),
                    new Version("1.0.0"),
                    Map.of("message", "hello"),
                    null
                )
            );

        Execution execution =
            manager.get(handle.executionId())
                .orElseThrow();

        // Execute
        ExecutionEngine engine =
            new ExecutionEngine(
                planRegistry,
                stateManager,
                executorRegistry,
                eventPublisher
            );

        engine.execute(execution);

        // Verify final state
        ExecutionState finalState =
            stateManager.get(handle.executionId())
                .orElseThrow();

        assertTrue(
            finalState.completedNodes().contains(
                new EntityId("start")
            )
        );

        assertTrue(
            finalState.completedNodes().contains(
                new EntityId("calc")
            )
        );

        assertTrue(
            finalState.completedNodes().contains(
                new EntityId("end")
            )
        );

        assertEquals(42, finalState.data().get("result"));

        // Verify events
        List<ExecutionEvent> events =
            eventPublisher.events();

        assertFalse(events.isEmpty());

        // Verify event sequence is monotonically increasing
        for (int i = 1; i < events.size(); i++) {
            assertTrue(
                events.get(i).sequence() >
                events.get(i - 1).sequence()
            );
        }

        // Verify event types
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
    }

    @Test
    void shouldHandleProviderFailure() {

        Schema schema = () -> "object";

        CapabilityDefinition capability =
            new CapabilityDefinition(
                new EntityId("failing.capability"),
                new Version("1.0.0"),
                "Failing",
                schema,
                schema,
                null
            );

        GraphDefinition graph =
            GraphBuilder
                .graph("fail-demo", "1.0.0", "Fail Demo")
                .start("start")
                .capability("fail", capability)
                .end("end")
                .connect("start", "fail")
                .connect("fail", "end")
                .build();

        DefaultGraphCompiler compiler =
            new DefaultGraphCompiler();

        Reference capRef = new Reference(
            new EntityId("failing.capability"),
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
            io.github.regalpine.agentforge.graph.NodeType.START,
            new StartNodeExecutor()
        );

        executorRegistry.register(
            io.github.regalpine.agentforge.graph.NodeType.END,
            new EndNodeExecutor()
        );

        CapabilityExecutor capabilityExecutor =
            new CapabilityExecutor(
                new SimpleCapabilityProviderResolver(
                    Map.of(
                        capRef,
                        mockProvider(
                            new CapabilityFailure(
                                "PROVIDER_ERROR",
                                "Something went wrong",
                                false
                            )
                        )
                    )
                )
            );

        executorRegistry.register(
            io.github.regalpine.agentforge.graph.NodeType.CAPABILITY,
            new CapabilityNodeExecutor(capabilityExecutor)
        );

        ExecutionManager manager =
            new ExecutionManager(planRegistry, stateManager);

        ExecutionHandle handle =
            manager.submit(
                new ExecutionRequest(
                    new EntityId("fail-demo"),
                    new Version("1.0.0"),
                    Map.of(),
                    null
                )
            );

        Execution execution =
            manager.get(handle.executionId())
                .orElseThrow();

        ExecutionEngine engine =
            new ExecutionEngine(
                planRegistry,
                stateManager,
                executorRegistry,
                eventPublisher
            );

        engine.execute(execution);

        // Verify that END was NOT reached
        ExecutionState finalState =
            stateManager.get(handle.executionId())
                .orElseThrow();

        assertFalse(
            finalState.completedNodes().contains(
                new EntityId("end")
            )
        );

        // Verify NODE_FAILED event was published
        List<String> eventTypes =
            eventPublisher.events().stream()
                .map(ExecutionEvent::type)
                .toList();

        assertTrue(
            eventTypes.contains("NODE_FAILED")
        );
    }
}
