package io.github.regalpine.agentforge.arch;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Modifier;
import java.lang.reflect.RecordComponent;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Verifies that Core API types follow the expected patterns:
 * - EntityId is a record
 * - Version is a record
 * - Reference is a record
 * - Key interfaces are public
 */
class ApiShapeTest {

    @Test
    void entityIdShouldBeRecord() {
        assertTrue(io.github.regalpine.agentforge.kernel.EntityId.class.isRecord(),
                "EntityId should be a Java record");
    }

    @Test
    void versionShouldBeRecord() {
        assertTrue(io.github.regalpine.agentforge.kernel.Version.class.isRecord(),
                "Version should be a Java record");
    }

    @Test
    void referenceShouldBeRecord() {
        assertTrue(io.github.regalpine.agentforge.kernel.Reference.class.isRecord(),
                "Reference should be a Java record");
    }

    @Test
    void coreEntityIdShouldHaveExpectedComponents() {
        RecordComponent[] components = io.github.regalpine.agentforge.kernel.EntityId.class.getRecordComponents();
        assertEquals(1, components.length, "EntityId should have exactly 1 component");
        assertEquals("value", components[0].getName());
    }

    @Test
    void coreVersionShouldHaveExpectedComponents() {
        RecordComponent[] components = io.github.regalpine.agentforge.kernel.Version.class.getRecordComponents();
        assertEquals(1, components.length, "Version should have exactly 1 component");
        assertEquals("value", components[0].getName());
    }

    @Test
    void keyInterfacesShouldBePublic() {
        assertTrue(Modifier.isPublic(
                io.github.regalpine.agentforge.capability.Capability.class.getModifiers()),
                "Capability interface must be public");
    }

    @Test
    void nodeTypeShouldBeEnum() {
        assertTrue(io.github.regalpine.agentforge.graph.NodeType.class.isEnum(),
                "NodeType should be an enum");
        assertTrue(io.github.regalpine.agentforge.graph.EdgeType.class.isEnum(),
                "EdgeType should be an enum");
    }

    @Test
    void executionStatusShouldBeEnum() {
        assertTrue(io.github.regalpine.agentforge.event.ExecutionStatus.class.isEnum(),
                "ExecutionStatus should be an enum");
    }
}
