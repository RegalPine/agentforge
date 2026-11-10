package io.github.regalpine.agentforge.sdk;

import io.github.regalpine.agentforge.sdk.bootstrap.*;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

/**
 * X0 Bootstrap tests.
 * AF-ENHANCE-01 §19-20: Lifecycle,
 * Dependency resolution, Cycle detection.
 */
class X0BootstrapTest {

    @Test
    void lifecycleStartsFromCreate() {
        BootstrapLifecycle lifecycle =
            new BootstrapLifecycle();

        assertEquals(
            BootstrapState.CREATE,
            lifecycle.currentState()
        );
    }

    @Test
    void lifecycleFullStart() {
        BootstrapLifecycle lifecycle =
            new BootstrapLifecycle();

        lifecycle.start();

        assertEquals(
            BootstrapState.RUNNING,
            lifecycle.currentState()
        );
    }

    @Test
    void lifecycleStop() {
        BootstrapLifecycle lifecycle =
            new BootstrapLifecycle();

        lifecycle.start();
        lifecycle.stop();

        assertEquals(
            BootstrapState.STOPPED,
            lifecycle.currentState()
        );
    }

    @Test
    void lifecycleInvalidTransitionThrows() {
        BootstrapLifecycle lifecycle =
            new BootstrapLifecycle();

        // CREATE → RUNNING is invalid
        assertThrows(
            BootstrapException.class,
            () -> lifecycle.transition(
                BootstrapState.RUNNING
            )
        );
    }

    @Test
    void lifecycleListenerNotified() {
        BootstrapLifecycle lifecycle =
            new BootstrapLifecycle();

        AtomicReference<BootstrapState>
            lastState = new AtomicReference<>();

        lifecycle.addListener(lastState::set);
        lifecycle.transition(BootstrapState.LOAD);

        assertEquals(
            BootstrapState.LOAD,
            lastState.get()
        );
    }

    @Test
    void dependencyGraphNoCycle() {
        DependencyGraph graph =
            new DependencyGraph();

        graph.addEdge("A", "B");
        graph.addEdge("B", "C");

        assertFalse(graph.hasCycle());
        assertDoesNotThrow(graph::validate);
    }

    @Test
    void dependencyGraphDetectsCycle() {
        DependencyGraph graph =
            new DependencyGraph();

        graph.addEdge("A", "B");
        graph.addEdge("B", "C");
        graph.addEdge("C", "A");

        assertTrue(graph.hasCycle());
        assertThrows(
            BootstrapException.class,
            graph::validate
        );
    }

    @Test
    void dependencyGraphSelfCycle() {
        DependencyGraph graph =
            new DependencyGraph();

        graph.addEdge("A", "A");

        assertTrue(graph.hasCycle());
    }

    @Test
    void dependencyGraphNodes() {
        DependencyGraph graph =
            new DependencyGraph();

        graph.addEdge("A", "B");
        graph.addEdge("A", "C");

        assertTrue(
            graph.nodes().contains("A")
        );
        assertTrue(
            graph.nodes().contains("B")
        );
        assertTrue(
            graph.nodes().contains("C")
        );
        assertEquals(2,
            graph.dependencies("A").size());
    }

    @Test
    void bootstrapStateTransitions() {
        assertTrue(
            BootstrapState.CREATE
                .canTransitionTo(BootstrapState.LOAD)
        );
        assertFalse(
            BootstrapState.CREATE
                .canTransitionTo(BootstrapState.RUNNING)
        );
        assertTrue(
            BootstrapState.READY
                .canTransitionTo(BootstrapState.RUNNING)
        );
        assertFalse(
            BootstrapState.STOPPED
                .canTransitionTo(BootstrapState.RUNNING)
        );
    }

    @Test
    void bootstrapStateProperties() {
        assertTrue(
            BootstrapState.STOPPED.isTerminal()
        );
        assertTrue(
            BootstrapState.FAILED.isTerminal()
        );
        assertFalse(
            BootstrapState.RUNNING.isTerminal()
        );
        assertTrue(
            BootstrapState.RUNNING.isActive()
        );
        assertTrue(
            BootstrapState.LOAD.isStarting()
        );
    }

    @Test
    void agentDefinitionParse() {
        String yaml = """
            id: researcher
            name: Researcher
            model:
              ref: gpt-4
            skills:
              - research
              - analysis
            tools:
              - web-search
            """;

        AgentDefinition def =
            AgentDefinitionParser.parseContent(
                yaml
            );

        assertEquals("researcher", def.id());
        assertEquals("Researcher", def.name());
        assertEquals("gpt-4", def.modelRef());
        assertEquals(2, def.skills().size());
        assertEquals(1, def.tools().size());
        assertTrue(def.hasModel());
        assertTrue(def.hasSkills());
        assertTrue(def.hasTools());
    }

    @Test
    void agentDefinitionToBuilder() {
        AgentDefinition def =
            new AgentDefinition(
                "test-agent",
                "Test",
                "default",
                List.of("skill-1"),
                List.of("tool-1")
            );

        AgentBuilder builder =
            AgentDefinitionParser.toBuilder(def);

        assertEquals("test-agent",
            builder.agentId());
        assertEquals(1,
            builder.skills().size());
        assertEquals(0,
            builder.policies().size());
    }
}
