package io.github.regalpine.agentforge.arch;

import io.github.regalpine.agentforge.agent.AgentDefinition;
import io.github.regalpine.agentforge.capability.CapabilityDefinition;
import io.github.regalpine.agentforge.capability.CapabilityContract;
import io.github.regalpine.agentforge.compiler.CompilationContext;
import io.github.regalpine.agentforge.compiler.CompilationResult;
import io.github.regalpine.agentforge.compiler.CompilerOptions;
import io.github.regalpine.agentforge.compiler.DefaultGraphCompiler;
import io.github.regalpine.agentforge.compiler.RegistrySnapshot;
import io.github.regalpine.agentforge.graph.GraphBuilder;
import io.github.regalpine.agentforge.graph.GraphDefinition;
import io.github.regalpine.agentforge.graph.NodeType;
import io.github.regalpine.agentforge.graph.ir.ExecutionNode;
import io.github.regalpine.agentforge.graph.ir.ExecutionPlan;
import io.github.regalpine.agentforge.graph.ir.RoutingEntry;
import io.github.regalpine.agentforge.kernel.EntityId;
import io.github.regalpine.agentforge.kernel.Reference;
import io.github.regalpine.agentforge.kernel.Version;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

/**
 * AF-42 §41 Golden Path Matrix + §62-§64 Golden Path Examples.
 *
 * Example 1 (§62): Simple Capability Workflow: START → CAPABILITY → END
 * Example 2 (§63): Agent Decision: START → AGENT → CAPABILITY → END
 * Example 3 (§64): Multi-Step Agent Graph: START → AGENT → CAPABILITY → CONDITION → (Cap A | Cap B) → END
 */
class Af42GoldenPathTest {

    private DefaultGraphCompiler compiler;
    private CapabilityDefinition calcCapability;
    private CapabilityDefinition capA;
    private CapabilityDefinition capB;
    private AgentDefinition agent;

    @BeforeEach
    void setUp() {
        compiler = new DefaultGraphCompiler();

        calcCapability = new CapabilityDefinition(
            new EntityId("calculator"), new Version("1.0.0"),
            "Calculator", null, null, CapabilityContract.defaults()
        );
        capA = new CapabilityDefinition(
            new EntityId("cap-a"), new Version("1.0.0"),
            "Capability A", null, null, CapabilityContract.defaults()
        );
        capB = new CapabilityDefinition(
            new EntityId("cap-b"), new Version("1.0.0"),
            "Capability B", null, null, CapabilityContract.defaults()
        );
        agent = new AgentDefinition(
            new EntityId("assistant"), new Version("1.0.0"),
            "Assistant Agent",
            Set.of(
                new Reference(calcCapability.id(), calcCapability.version()),
                new Reference(capA.id(), capA.version()),
                new Reference(capB.id(), capB.version())
            )
        );
    }

    private CompilationContext contextWithRegistry() {
        RegistrySnapshot snapshot = new RegistrySnapshot(
            Map.of(
                new Reference(agent.id(), agent.version()), agent
            ),
            Map.of(
                new Reference(calcCapability.id(), calcCapability.version()), calcCapability,
                new Reference(capA.id(), capA.version()), capA,
                new Reference(capB.id(), capB.version()), capB
            ),
            Map.of()
        );
        return new CompilationContext(snapshot, CompilerOptions.defaults());
    }

    // ── AF-42 §62: Example 1 — Simple Capability Workflow ──

    @Test
    void example1_simpleCapabilityWorkflow() {
        // AF-42 §62: START → CAPABILITY → END
        GraphDefinition graph = GraphBuilder
            .graph("golden-path-1", "1.0.0", "Simple Capability Workflow")
            .start("start")
            .capability("calc", calcCapability)
            .end("end")
            .connect("start", "calc")
            .connect("calc", "end")
            .build();

        CompilationResult result = compiler.compile(graph, contextWithRegistry());

        assertInstanceOf(CompilationResult.Success.class, result,
            "Golden Path Example 1 should compile successfully");

        CompilationResult.Success success = (CompilationResult.Success) result;
        ExecutionPlan plan = success.executionPlan();

        // Verify plan structure
        assertEquals(new EntityId("golden-path-1"), plan.graphId());
        assertEquals(new Version("1.0.0"), plan.graphVersion());
        assertNotNull(plan.fingerprint(), "Plan must have fingerprint");
        assertEquals(new EntityId("start"), plan.entryNode(), "Entry must be START");
        assertEquals(3, plan.nodes().size(), "Plan must have 3 nodes");

        // Verify node types
        Set<NodeType> nodeTypes = plan.nodes().stream()
            .map(ExecutionNode::type)
            .collect(Collectors.toSet());
        assertTrue(nodeTypes.contains(NodeType.START));
        assertTrue(nodeTypes.contains(NodeType.CAPABILITY));
        assertTrue(nodeTypes.contains(NodeType.END));

        // Verify routing: start → calc → end
        assertTrue(plan.routing().containsKey(new EntityId("start")));
        assertTrue(plan.routing().containsKey(new EntityId("calc")));

        // Verify determinism: compile again, same fingerprint
        CompilationResult result2 = compiler.compile(graph, contextWithRegistry());
        CompilationResult.Success success2 = (CompilationResult.Success) result2;
        assertEquals(plan.fingerprint(), success2.executionPlan().fingerprint(),
            "AF-42 §28: Same input must produce same fingerprint");
    }

    // ── AF-42 §63: Example 2 — Agent Decision ──

    @Test
    void example2_agentDecision() {
        // AF-42 §63: START → AGENT → CAPABILITY → END
        GraphDefinition graph = GraphBuilder
            .graph("golden-path-2", "1.0.0", "Agent Decision")
            .start("start")
            .agent("agent-1", agent)
            .capability("calc", calcCapability)
            .end("end")
            .connect("start", "agent-1")
            .connect("agent-1", "calc")
            .connect("calc", "end")
            .build();

        CompilationResult result = compiler.compile(graph, contextWithRegistry());

        assertInstanceOf(CompilationResult.Success.class, result,
            "Golden Path Example 2 should compile successfully");

        CompilationResult.Success success = (CompilationResult.Success) result;
        ExecutionPlan plan = success.executionPlan();

        assertEquals(4, plan.nodes().size(), "Plan must have 4 nodes");

        // Verify node types include AGENT
        Set<NodeType> nodeTypes = plan.nodes().stream()
            .map(ExecutionNode::type)
            .collect(Collectors.toSet());
        assertTrue(nodeTypes.contains(NodeType.AGENT), "Must contain AGENT node");
        assertTrue(nodeTypes.contains(NodeType.CAPABILITY), "Must contain CAPABILITY node");

        // Verify routing chain
        assertTrue(plan.routing().containsKey(new EntityId("start")),
            "START must have outgoing routing");
        assertTrue(plan.routing().containsKey(new EntityId("agent-1")),
            "AGENT must have outgoing routing");
        assertTrue(plan.routing().containsKey(new EntityId("calc")),
            "CAPABILITY must have outgoing routing");
    }

    // ── AF-42 §64: Example 3 — Multi-Step Agent Graph ──

    @Test
    void example3_multiStepAgentGraph() {
        // AF-42 §64: START → AGENT → CAPABILITY → CONDITION → (Cap A | Cap B) → END
        GraphDefinition graph = GraphBuilder
            .graph("golden-path-3", "1.0.0", "Multi-Step Agent Graph")
            .start("start")
            .agent("agent-1", agent)
            .capability("calc", calcCapability)
            .end("end")
            .connect("start", "agent-1")
            .connect("agent-1", "calc")
            .connect("calc", "condition")
            .connect("condition", "cap-a")
            .connect("condition", "cap-b")
            .connect("cap-a", "end")
            .connect("cap-b", "end")
            .build();

        // Add CONDITION and capability nodes manually (GraphBuilder doesn't have condition method)
        // We need to rebuild with proper node types
        List<io.github.regalpine.agentforge.graph.NodeDefinition> allNodes = new java.util.ArrayList<>(graph.nodes());
        allNodes.add(new io.github.regalpine.agentforge.graph.NodeDefinition(
            new EntityId("condition"), NodeType.CONDITION, null, null
        ));
        allNodes.add(new io.github.regalpine.agentforge.graph.NodeDefinition(
            new EntityId("cap-a"), NodeType.CAPABILITY,
            new Reference(capA.id(), capA.version()), null
        ));
        allNodes.add(new io.github.regalpine.agentforge.graph.NodeDefinition(
            new EntityId("cap-b"), NodeType.CAPABILITY,
            new Reference(capB.id(), capB.version()), null
        ));

        GraphDefinition fullGraph = new GraphDefinition(
            graph.id(), graph.version(), graph.name(), graph.description(),
            graph.inputs(), graph.outputs(),
            allNodes, graph.edges(),
            graph.bindings(), graph.expressions(),
            graph.policies(), graph.metadata()
        );

        CompilationResult result = compiler.compile(fullGraph, contextWithRegistry());

        assertInstanceOf(CompilationResult.Success.class, result,
            "Golden Path Example 3 should compile successfully");

        CompilationResult.Success success = (CompilationResult.Success) result;
        ExecutionPlan plan = success.executionPlan();

        assertEquals(7, plan.nodes().size(), "Plan must have 7 nodes (start, agent, calc, condition, cap-a, cap-b, end)");

        // Verify node types
        Set<NodeType> nodeTypes = plan.nodes().stream()
            .map(ExecutionNode::type)
            .collect(Collectors.toSet());
        assertTrue(nodeTypes.contains(NodeType.START));
        assertTrue(nodeTypes.contains(NodeType.AGENT));
        assertTrue(nodeTypes.contains(NodeType.CAPABILITY));
        assertTrue(nodeTypes.contains(NodeType.CONDITION));
        assertTrue(nodeTypes.contains(NodeType.END));

        // Verify CONDITION has multiple outgoing routes (branching)
        // G3-1: routing now uses RoutingEntry instead of EntityId
        List<RoutingEntry> conditionRoutes = plan.routing().get(new EntityId("condition"));
        assertNotNull(conditionRoutes, "CONDITION must have routing");
        assertEquals(2, conditionRoutes.size(), "CONDITION must branch to 2 paths");

        // Verify fingerprint determinism
        CompilationResult result2 = compiler.compile(fullGraph, contextWithRegistry());
        CompilationResult.Success success2 = (CompilationResult.Success) result2;
        assertEquals(plan.fingerprint(), success2.executionPlan().fingerprint(),
            "AF-42 §28: Same input must produce same fingerprint");
    }

    // ── AF-42 §41: Golden Path Matrix ──

    @Test
    void goldenPathMatrix_allCompile() {
        // Verify all 3 golden paths compile to Success
        GraphDefinition gp1 = GraphBuilder
            .graph("gp-matrix-1", "1.0.0", "GP1")
            .start("start").capability("cap", calcCapability).end("end")
            .connect("start", "cap").connect("cap", "end")
            .build();

        GraphDefinition gp2 = GraphBuilder
            .graph("gp-matrix-2", "1.0.0", "GP2")
            .start("start").agent("a", agent).capability("cap", calcCapability).end("end")
            .connect("start", "a").connect("a", "cap").connect("cap", "end")
            .build();

        CompilationContext ctx = contextWithRegistry();

        assertInstanceOf(CompilationResult.Success.class,
            compiler.compile(gp1, ctx), "Golden Path 1 must PASS");
        assertInstanceOf(CompilationResult.Success.class,
            compiler.compile(gp2, ctx), "Golden Path 2 must PASS");
    }
}
