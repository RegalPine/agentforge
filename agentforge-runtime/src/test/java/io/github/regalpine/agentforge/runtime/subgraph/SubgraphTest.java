package io.github.regalpine.agentforge.runtime.subgraph;

import io.github.regalpine.agentforge.event.ExecutionId;

import io.github.regalpine.agentforge.graph.NodeType;
import io.github.regalpine.agentforge.graph.ir.ExecutionNode;
import io.github.regalpine.agentforge.kernel.EntityId;
import io.github.regalpine.agentforge.runtime.*;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for G-03: SUBGRAPH sub-graph execution.
 */
class SubgraphTest {

    @Test
    void shouldCreateSubgraphResult() {
        ExecutionId childId = new ExecutionId("parent-1/sub/node-1");
        SubgraphResult result = new SubgraphResult(
                childId, new EntityId("node-1"),
                SubgraphStatus.COMPLETED,
                Map.of("output", "value"),
                Instant.now()
        );

        assertEquals(SubgraphStatus.COMPLETED, result.status());
        assertEquals("value", result.outputs().get("output"));
    }

    @Test
    void shouldResolveCompletedResult() {
        ExecutionManager manager = new ExecutionManager(
                new InMemoryExecutionPlanRegistry(), new InMemoryStateManager()
        );
        SubgraphExecutor executor = new SubgraphExecutor(manager);

        SubgraphResult completed = new SubgraphResult(
                new ExecutionId("child-1"), new EntityId("node-1"),
                SubgraphStatus.COMPLETED, Map.of("key", "val"), Instant.now()
        );

        Map<String, Object> resolved = executor.resolveResult(completed);
        assertEquals("val", resolved.get("key"));
    }

    @Test
    void shouldThrowOnFailedResult() {
        ExecutionManager manager = new ExecutionManager(
                new InMemoryExecutionPlanRegistry(), new InMemoryStateManager()
        );
        SubgraphExecutor executor = new SubgraphExecutor(manager);

        SubgraphResult failed = new SubgraphResult(
                new ExecutionId("child-2"), new EntityId("node-2"),
                SubgraphStatus.FAILED, Map.of(), Instant.now()
        );

        assertThrows(SubgraphExecutionException.class, () -> executor.resolveResult(failed));
    }

    @Test
    void shouldThrowOnIncompleteResult() {
        ExecutionManager manager = new ExecutionManager(
                new InMemoryExecutionPlanRegistry(), new InMemoryStateManager()
        );
        SubgraphExecutor executor = new SubgraphExecutor(manager);

        SubgraphResult running = new SubgraphResult(
                new ExecutionId("child-3"), new EntityId("node-3"),
                SubgraphStatus.RUNNING, Map.of(), Instant.now()
        );

        assertThrows(SubgraphExecutionException.class, () -> executor.resolveResult(running));
    }

    @Test
    void shouldRejectNonSubgraphNode() {
        ExecutionManager manager = new ExecutionManager(
                new InMemoryExecutionPlanRegistry(), new InMemoryStateManager()
        );
        SubgraphExecutor subgraphExec = new SubgraphExecutor(manager);

        ExecutionNode actionNode = new ExecutionNode(
                new EntityId("action-1"), NodeType.ACTION, Map.of(), Map.of()
        );

        SubgraphNodeExecutor nodeExec = new SubgraphNodeExecutor(subgraphExec, null);
        NodeExecutionResult result = nodeExec.execute(actionNode, null);
        assertInstanceOf(NodeFailed.class, result);
        NodeFailed failed = (NodeFailed) result;
        assertEquals("SUBGRAPH_TYPE_MISMATCH", failed.code());
    }
}
