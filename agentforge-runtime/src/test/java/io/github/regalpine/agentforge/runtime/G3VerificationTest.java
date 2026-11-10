package io.github.regalpine.agentforge.runtime;

import io.github.regalpine.agentforge.capability.*;
import io.github.regalpine.agentforge.compiler.*;
import io.github.regalpine.agentforge.graph.*;
import io.github.regalpine.agentforge.graph.ir.*;
import io.github.regalpine.agentforge.kernel.EntityId;
import io.github.regalpine.agentforge.kernel.Version;
import io.github.regalpine.agentforge.agent.*;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * G3 verification tests.
 * Covers: routing semantics, fingerprint
 * coverage, action execution port,
 * nested JSON parsing.
 */
class G3VerificationTest {

    // ── G3-1: Edge semantics routing ──

    @Test
    void shouldRouteOnSuccessViaNormalEdge() {
        ExecutionPlan plan = buildLinearPlan(
            EdgeType.NORMAL
        );
        ExecutionRouter router = new ExecutionRouter();

        EntityId next = router.nextOnSuccess(
            plan, new EntityId("start"), Map.of()
        );

        assertNotNull(next);
        assertEquals("end", next.value());
    }

    @Test
    void shouldRouteOnFailureViaErrorEdge() {
        ExecutionPlan plan = buildErrorPlan();
        ExecutionRouter router = new ExecutionRouter();

        EntityId next = router.nextOnFailure(
            plan, new EntityId("start")
        );

        assertNotNull(next);
        assertEquals(
            "error-handler", next.value()
        );
    }

    @Test
    void shouldNotRouteOnSuccessViaErrorEdge() {
        ExecutionPlan plan = buildErrorPlan();
        ExecutionRouter router = new ExecutionRouter();

        EntityId next = router.nextOnSuccess(
            plan, new EntityId("start"), Map.of()
        );

        assertNull(next);
    }

    @Test
    void shouldRouteConditionalOnMatch() {
        ExecutionPlan plan = buildConditionalPlan();
        ExecutionRouter router = new ExecutionRouter();

        EntityId next = router.nextOnSuccess(
            plan, new EntityId("start"),
            Map.of("approved", true)
        );

        assertNotNull(next);
        assertEquals("approved-path", next.value());
    }

    @Test
    void shouldSkipConditionalOnNoMatch() {
        ExecutionPlan plan = buildConditionalPlan();
        ExecutionRouter router = new ExecutionRouter();

        EntityId next = router.nextOnSuccess(
            plan, new EntityId("start"),
            Map.of("approved", false)
        );

        assertNotNull(next);
        assertEquals("normal-path", next.value());
    }

    // ── G3-2: Fingerprint coverage ──

    @Test
    void fingerprintShouldChangeWithNodeConfig() {
        GraphDefinition g1 = buildSimpleGraph(
            Map.of("key", "value1")
        );
        GraphDefinition g2 = buildSimpleGraph(
            Map.of("key", "value2")
        );

        DefaultGraphCompiler compiler =
            new DefaultGraphCompiler();
        CompilationContext ctx =
            new CompilationContext(null, null);
        CompilationResult r1 = compiler.compile(g1, ctx);
        CompilationResult r2 = compiler.compile(g2, ctx);

        assertInstanceOf(
            CompilationResult.Success.class, r1
        );
        assertInstanceOf(
            CompilationResult.Success.class, r2
        );

        String fp1 = ((CompilationResult.Success) r1)
            .executionPlan().fingerprint();
        String fp2 = ((CompilationResult.Success) r2)
            .executionPlan().fingerprint();

        assertNotEquals(fp1, fp2,
            "Different config should produce "
                + "different fingerprints"
        );
    }

    @Test
    void fingerprintShouldChangeWithEdgeCondition() {
        GraphDefinition g1 = buildGraphWithCondition(
            "approved == true"
        );
        GraphDefinition g2 = buildGraphWithCondition(
            "approved == false"
        );

        DefaultGraphCompiler compiler =
            new DefaultGraphCompiler();
        CompilationContext ctx =
            new CompilationContext(null, null);
        CompilationResult r1 = compiler.compile(g1, ctx);
        CompilationResult r2 = compiler.compile(g2, ctx);

        String fp1 = ((CompilationResult.Success) r1)
            .executionPlan().fingerprint();
        String fp2 = ((CompilationResult.Success) r2)
            .executionPlan().fingerprint();

        assertNotEquals(fp1, fp2,
            "Different conditions should produce "
                + "different fingerprints"
        );
    }

    @Test
    void fingerprintShouldBeStableForSameGraph() {
        GraphDefinition g1 = buildSimpleGraph(
            Map.of("key", "value")
        );
        GraphDefinition g2 = buildSimpleGraph(
            Map.of("key", "value")
        );

        DefaultGraphCompiler compiler =
            new DefaultGraphCompiler();
        CompilationContext ctx =
            new CompilationContext(null, null);
        CompilationResult r1 = compiler.compile(g1, ctx);
        CompilationResult r2 = compiler.compile(g2, ctx);

        String fp1 = ((CompilationResult.Success) r1)
            .executionPlan().fingerprint();
        String fp2 = ((CompilationResult.Success) r2)
            .executionPlan().fingerprint();

        assertEquals(fp1, fp2,
            "Same graph compiled twice should "
                + "have same fingerprint"
        );
    }

    // ── G3-3: Action execution port ──

    @Test
    void capabilityDecisionShouldTriggerActionPort() {
        List<String> invoked = new ArrayList<>();

        ActionExecutionPort port = decision -> {
            invoked.add(decision.capabilityId());
            return new CapabilitySuccess(
                Map.of("result", "done")
            );
        };

        AgentContext context = new AgentContext(
            Map.of("goal", "test"),
            "test",
            Map.of(), List.of(), Map.of(), Map.of()
        );

        AgentDecisionEngine finishEngine =
            new AgentDecisionEngine() {
                int count = 0;
                @Override
                public AgentDecision decide(
                    AgentDecisionContext ctx
                ) {
                    if (count++ == 0) {
                        return new CapabilityDecision(
                            DecisionType.CALL_CAPABILITY,
                            "test-cap",
                            Map.of(), "test"
                        );
                    }
                    return new FinishDecision(
                        Map.of("done", true)
                    );
                }
            };

        AgentLoop loop = AgentLoop.withDefaults(
            finishEngine, context, port
        );
        AgentResult result = loop.run();

        assertEquals(
            AgentResultStatus.COMPLETED,
            result.status()
        );
        assertEquals(1, invoked.size());
        assertEquals("test-cap", invoked.get(0));
    }

    @Test
    void noOpPortShouldReturnFailure() {
        ActionExecutionPort port =
            ActionExecutionPort.noOp();

        CapabilityDecision decision =
            new CapabilityDecision(
                DecisionType.CALL_CAPABILITY,
                "missing-cap",
                Map.of(), "test"
            );

        CapabilityResult result =
            port.execute(decision);

        assertInstanceOf(
            CapabilityFailure.class, result
        );
        assertEquals(
            "NO_ACTION_PORT",
            ((CapabilityFailure) result).code()
        );
    }

    // ── Helper methods ──

    private ExecutionPlan buildLinearPlan(
        EdgeType edgeType
    ) {
        EntityId startId = new EntityId("start");
        EntityId endId = new EntityId("end");

        ExecutionNode startNode = new ExecutionNode(
            startId, NodeType.START,
            Map.of(), Map.of()
        );
        ExecutionNode endNode = new ExecutionNode(
            endId, NodeType.END,
            Map.of(), Map.of()
        );

        Map<EntityId, List<RoutingEntry>> routing =
            new HashMap<>();
        routing.put(startId, List.of(
            new RoutingEntry(
                endId, edgeType, null
            )
        ));

        return new ExecutionPlan(
            new EntityId("graph"),
            new Version("1.0.0"),
            "fp",
            startId,
            List.of(startNode, endNode),
            routing,
            Map.of()
        );
    }

    private ExecutionPlan buildErrorPlan() {
        EntityId startId = new EntityId("start");
        EntityId errorId =
            new EntityId("error-handler");

        ExecutionNode startNode = new ExecutionNode(
            startId, NodeType.START,
            Map.of(), Map.of()
        );
        ExecutionNode errorNode = new ExecutionNode(
            errorId, NodeType.CAPABILITY,
            Map.of(), Map.of()
        );

        Map<EntityId, List<RoutingEntry>> routing =
            new HashMap<>();
        routing.put(startId, List.of(
            new RoutingEntry(
                errorId, EdgeType.ERROR, null
            )
        ));

        return new ExecutionPlan(
            new EntityId("graph"),
            new Version("1.0.0"),
            "fp",
            startId,
            List.of(startNode, errorNode),
            routing,
            Map.of()
        );
    }

    private ExecutionPlan buildConditionalPlan() {
        EntityId startId = new EntityId("start");
        EntityId approvedId =
            new EntityId("approved-path");
        EntityId normalId =
            new EntityId("normal-path");

        ExecutionNode startNode = new ExecutionNode(
            startId, NodeType.START,
            Map.of(), Map.of()
        );

        Map<EntityId, List<RoutingEntry>> routing =
            new HashMap<>();
        routing.put(startId, List.of(
            RoutingEntry.conditional(
                approvedId, "approved"
            ),
            RoutingEntry.normal(normalId)
        ));

        return new ExecutionPlan(
            new EntityId("graph"),
            new Version("1.0.0"),
            "fp",
            startId,
            List.of(startNode),
            routing,
            Map.of()
        );
    }

    private GraphDefinition buildSimpleGraph(
        Map<String, Object> config
    ) {
        NodeDefinition startNode =
            new NodeDefinition(
                new EntityId("start"),
                NodeType.START,
                null, null
            );

        NodeDefinition endNode =
            new NodeDefinition(
                new EntityId("end"),
                NodeType.END,
                null, config
            );

        EdgeDefinition edge = new EdgeDefinition(
            new EntityId("e1"),
            new EntityId("start"),
            new EntityId("end"),
            EdgeType.NORMAL,
            null
        );

        return new GraphDefinition(
            new EntityId("test-graph"),
            new Version("1.0.0"),
            "test",
            "test graph",
            Map.of(), Map.of(),
            List.of(startNode, endNode),
            List.of(edge),
            List.of(), List.of(),
            Map.of(), Map.of()
        );
    }

    private GraphDefinition buildGraphWithCondition(
        String condition
    ) {
        NodeDefinition startNode =
            new NodeDefinition(
                new EntityId("start"),
                NodeType.START,
                null, null
            );

        NodeDefinition endNode =
            new NodeDefinition(
                new EntityId("end"),
                NodeType.END,
                null, null
            );

        EdgeDefinition edge = new EdgeDefinition(
            new EntityId("e1"),
            new EntityId("start"),
            new EntityId("end"),
            EdgeType.CONDITIONAL,
            condition
        );

        return new GraphDefinition(
            new EntityId("test-graph"),
            new Version("1.0.0"),
            "test",
            "test graph",
            Map.of(), Map.of(),
            List.of(startNode, endNode),
            List.of(edge),
            List.of(), List.of(),
            Map.of(), Map.of()
        );
    }

    private GraphDefinition buildTwoNodeGraph(
        String name1, String name2
    ) {
        NodeDefinition n1 = new NodeDefinition(
            new EntityId(name1),
            NodeType.START,
            null,
            Map.of("order", name1)
        );

        NodeDefinition n2 = new NodeDefinition(
            new EntityId(name2),
            NodeType.END,
            null,
            Map.of("order", name2)
        );

        EdgeDefinition edge = new EdgeDefinition(
            new EntityId("e1"),
            new EntityId(name1),
            new EntityId(name2),
            EdgeType.NORMAL,
            null
        );

        return new GraphDefinition(
            new EntityId("test-graph"),
            new Version("1.0.0"),
            "test",
            "test graph",
            Map.of(), Map.of(),
            List.of(n1, n2),
            List.of(edge),
            List.of(), List.of(),
            Map.of(), Map.of()
        );
    }
}
