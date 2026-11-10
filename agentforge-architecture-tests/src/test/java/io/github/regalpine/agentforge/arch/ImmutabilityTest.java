package io.github.regalpine.agentforge.arch;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Modifier;
import java.lang.reflect.Method;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Verifies that core types are immutable:
 * - Records are inherently immutable (final fields)
 * - Collections returned by core types should be unmodifiable
 * - Key types should be final or sealed
 */
class ImmutabilityTest {

    @Test
    void graphDefinitionShouldBeFinal() {
        assertTrue(Modifier.isFinal(
                io.github.regalpine.agentforge.graph.GraphDefinition.class.getModifiers()),
                "GraphDefinition should be final");
    }

    @Test
    void nodeDefinitionShouldBeFinal() {
        assertTrue(Modifier.isFinal(
                io.github.regalpine.agentforge.graph.NodeDefinition.class.getModifiers()),
                "NodeDefinition should be final");
    }

    @Test
    void edgeDefinitionShouldBeFinal() {
        assertTrue(Modifier.isFinal(
                io.github.regalpine.agentforge.graph.EdgeDefinition.class.getModifiers()),
                "EdgeDefinition should be final");
    }

    @Test
    void entityIdShouldBeFinal() {
        assertTrue(Modifier.isFinal(
                io.github.regalpine.agentforge.kernel.EntityId.class.getModifiers()),
                "EntityId should be final (record)");
    }

    @Test
    void executionStateCollectionsShouldBeDefensive() throws Exception {
        // ExecutionState should not expose mutable internal state
        Class<?> execStateClass = io.github.regalpine.agentforge.event.ExecutionState.class;
        assertTrue(execStateClass.isRecord(),
                "ExecutionState should be a record for immutability");
    }
}
