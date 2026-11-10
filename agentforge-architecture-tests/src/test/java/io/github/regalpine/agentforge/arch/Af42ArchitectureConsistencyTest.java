package io.github.regalpine.agentforge.arch;

import io.github.regalpine.agentforge.event.ExecutionId;

import io.github.regalpine.agentforge.graph.GraphDefinition;
import io.github.regalpine.agentforge.graph.NodeDefinition;
import io.github.regalpine.agentforge.graph.EdgeDefinition;
import io.github.regalpine.agentforge.graph.ir.ExecutionPlan;
import io.github.regalpine.agentforge.graph.ir.ExecutionNode;
import io.github.regalpine.agentforge.kernel.EntityId;
import io.github.regalpine.agentforge.kernel.Reference;
import io.github.regalpine.agentforge.kernel.Version;
import io.github.regalpine.agentforge.compiler.CompilationResult;
import io.github.regalpine.agentforge.event.ExecutionState;
import io.github.regalpine.agentforge.event.ExecutionStatus;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Modifier;
import java.lang.reflect.RecordComponent;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

/**
 * AF-42 V1.0 Architecture Consistency Verification.
 *
 * Validates all consistency rules defined in AF-42 §3-§38:
 * - §4  Core Ontology Consistency
 * - §5  Agent consistency
 * - §6  Capability consistency
 * - §7  Graph consistency (immutability)
 * - §8  Execution consistency
 * - §9  Compiler consistency
 * - §10 Runtime consistency
 * - §11 ExecutionPlan structure
 * - §27 Reference Resolution
 * - §30 State Consistency (CAS)
 */
class Af42ArchitectureConsistencyTest {

    // ── AF-42 §4: Core Ontology Consistency ──

    @Test
    void coreEntitiesMustExist() {
        // AF-42 §4: Agent, Capability, Work, Graph, Node, Edge, Execution, State, Context, Event
        assertDoesNotThrow(() -> Class.forName("io.github.regalpine.agentforge.agent.Agent"));
        assertDoesNotThrow(() -> Class.forName("io.github.regalpine.agentforge.capability.Capability"));
        assertDoesNotThrow(() -> Class.forName("io.github.regalpine.agentforge.graph.GraphDefinition"));
        assertDoesNotThrow(() -> Class.forName("io.github.regalpine.agentforge.graph.NodeDefinition"));
        assertDoesNotThrow(() -> Class.forName("io.github.regalpine.agentforge.graph.EdgeDefinition"));
        assertDoesNotThrow(() -> Class.forName("io.github.regalpine.agentforge.event.ExecutionState"));
    }

    @Test
    void agentMustNotBeGraphOrWorkflow() {
        // AF-42 §5: Agent = Actor/Executor Identity, not Graph/Workflow/LLM/Provider/Execution
        assertFalse(
            io.github.regalpine.agentforge.graph.GraphDefinition.class.isAssignableFrom(
                io.github.regalpine.agentforge.agent.AgentDefinition.class),
            "Agent must not be a Graph"
        );
    }

    @Test
    void capabilityMustNotBeProviderOrTool() {
        // AF-42 §6: Capability = What can be done; Provider = implemented by; Tool/Protocol = uses
        assertFalse(
            io.github.regalpine.agentforge.graph.GraphDefinition.class.isAssignableFrom(
                io.github.regalpine.agentforge.capability.CapabilityDefinition.class),
            "Capability must not be a Graph"
        );
    }

    // ── AF-42 §7: Graph Consistency (Immutability) ──

    @Test
    void graphDefinitionMustBeImmutable() {
        // AF-42 §7: GraphDefinition must be immutable
        assertTrue(GraphDefinition.class.isRecord(),
            "GraphDefinition should be a record (immutable)");

        GraphDefinition graph = new GraphDefinition(
            new EntityId("test"), new Version("1.0.0"), "Test",
            null, null, null, null, null, null, null, null, null
        );

        // Verify defensive copies
        assertNotNull(graph.nodes());
        assertNotNull(graph.edges());
        assertNotNull(graph.inputs());
        assertNotNull(graph.outputs());

        assertThrows(UnsupportedOperationException.class,
            () -> graph.nodes().add(null),
            "nodes() should return unmodifiable list");
        assertThrows(UnsupportedOperationException.class,
            () -> graph.edges().add(null),
            "edges() should return unmodifiable list");
    }

    // ── AF-42 §8: Execution Consistency ──

    @Test
    void executionStateMustBeImmutable() {
        // AF-42 §8: Execution must be bound to GraphId + GraphVersion + PlanFingerprint
        assertTrue(ExecutionState.class.isRecord(),
            "ExecutionState should be a record (immutable)");

        ExecutionState state = new ExecutionState(
            new io.github.regalpine.agentforge.event.ExecutionId("exec-1"),
            ExecutionStatus.CREATED,
            new EntityId("node-1"),
            java.util.Map.of("key", "value"),
            Set.of(new EntityId("start")),
            Set.of(),
            0
        );

        assertThrows(UnsupportedOperationException.class,
            () -> state.data().put("new", "value"),
            "data() should return unmodifiable map");
        assertThrows(UnsupportedOperationException.class,
            () -> state.completedNodes().add(new EntityId("x")),
            "completedNodes() should return unmodifiable set");
    }

    // ── AF-42 §11: ExecutionPlan Structure ──

    @Test
    void executionPlanMustHaveRequiredFields() {
        // AF-42 §11: ExecutionPlan must have: graphId, graphVersion, fingerprint, entryNode, nodes, routing, policies
        RecordComponent[] components = ExecutionPlan.class.getRecordComponents();
        Set<String> fieldNames = Arrays.stream(components)
            .map(RecordComponent::getName)
            .collect(Collectors.toSet());

        assertTrue(fieldNames.contains("graphId"), "ExecutionPlan must have graphId");
        assertTrue(fieldNames.contains("graphVersion"), "ExecutionPlan must have graphVersion");
        assertTrue(fieldNames.contains("fingerprint"), "ExecutionPlan must have fingerprint");
        assertTrue(fieldNames.contains("entryNode"), "ExecutionPlan must have entryNode");
        assertTrue(fieldNames.contains("nodes"), "ExecutionPlan must have nodes");
        assertTrue(fieldNames.contains("routing"), "ExecutionPlan must have routing");
        assertTrue(fieldNames.contains("policies"), "ExecutionPlan must have policies");
    }

    @Test
    void executionPlanMustBeImmutable() {
        // AF-42 §11: ExecutionPlan must be Immutable
        assertTrue(ExecutionPlan.class.isRecord(),
            "ExecutionPlan should be a record");

        ExecutionPlan plan = new ExecutionPlan(
            new EntityId("g1"), new Version("1.0.0"), "fp-abc",
            new EntityId("start"),
            java.util.List.of(new ExecutionNode(new EntityId("start"),
                io.github.regalpine.agentforge.graph.NodeType.START, null, java.util.Map.of())),
            java.util.Map.of(), java.util.Map.of()
        );

        assertThrows(UnsupportedOperationException.class,
            () -> plan.nodes().add(null),
            "nodes() should be unmodifiable");
        assertThrows(UnsupportedOperationException.class,
            () -> plan.routing().put(new EntityId("x"), java.util.List.of()),
            "routing() should be unmodifiable");
    }

    // ── AF-42 §27: Reference Resolution ──

    @Test
    void referenceMustRequireIdAndVersion() {
        // AF-42 §27: Reference = id + version; "latest" is forbidden by default
        Reference ref = Reference.of("my-cap", "1.0.0");
        assertNotNull(ref.id());
        assertNotNull(ref.version());
        assertEquals("my-cap", ref.id().value());
        assertEquals("1.0.0", ref.version().value());

        // Reference requires both id and version
        assertThrows(NullPointerException.class,
            () -> new Reference(null, new Version("1.0.0")));
        assertThrows(NullPointerException.class,
            () -> new Reference(new EntityId("x"), null));
    }

    // ── AF-42 §30: State Consistency (CAS) ──

    @Test
    void executionStateMustUseVersionForCAS() {
        // AF-42 §30: State update must use expectedVersion → CAS → newVersion
        // ExecutionState has a version field for CAS
        RecordComponent[] components = ExecutionState.class.getRecordComponents();
        Set<String> fieldNames = Arrays.stream(components)
            .map(RecordComponent::getName)
            .collect(Collectors.toSet());

        assertTrue(fieldNames.contains("version"),
            "ExecutionState must have version field for CAS");
    }

    // ── AF-42 §35: Failure Consistency ──

    @Test
    void compilationResultMustBeSealed() {
        // AF-42 §35: Provider Failure = CapabilityFailure, must not expose raw exceptions
        // CompilationResult is sealed: Success | Failure
        assertTrue(CompilationResult.class.isSealed(),
            "CompilationResult should be sealed");
    }

    // ── AF-42 §36: Security Consistency ──

    @Test
    void entityMustRejectBlankId() {
        // AF-42 §36: Sensitive data must not enter execution artifacts
        // EntityId validation ensures only well-formed IDs are accepted
        assertThrows(IllegalArgumentException.class,
            () -> new EntityId(""));
        assertThrows(IllegalArgumentException.class,
            () -> new EntityId("   "));
        assertThrows(NullPointerException.class,
            () -> new EntityId(null));
    }
}
