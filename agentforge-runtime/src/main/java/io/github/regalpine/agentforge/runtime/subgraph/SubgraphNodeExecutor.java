package io.github.regalpine.agentforge.runtime.subgraph;


import io.github.regalpine.agentforge.graph.GraphDefinition;
import io.github.regalpine.agentforge.graph.NodeType;
import io.github.regalpine.agentforge.graph.ir.ExecutionNode;
import io.github.regalpine.agentforge.runtime.*;

import java.util.Map;

/**
 * NodeExecutor implementation for SUBGRAPH NodeType.
 * Delegates to {@link SubgraphExecutor} for child graph execution.
 *
 * <p>G-03: SUBGRAPH node executor.
 */
public final class SubgraphNodeExecutor implements NodeExecutor {

    private final SubgraphExecutor subgraphExecutor;
    private final GraphDefinition childGraph;

    public SubgraphNodeExecutor(SubgraphExecutor subgraphExecutor, GraphDefinition childGraph) {
        this.subgraphExecutor = subgraphExecutor;
        this.childGraph = childGraph;
    }

    @Override
    public NodeExecutionResult execute(ExecutionNode node, NodeExecutionContext context) {
        if (node.type() != NodeType.SUBGRAPH) {
            return new NodeFailed("SUBGRAPH_TYPE_MISMATCH",
                    "SubgraphNodeExecutor can only handle SUBGRAPH nodes, got: " + node.type(), false);
        }

        try {
            Map<String, Object> inputs = node.config();
            SubgraphResult result = subgraphExecutor.execute(
                    context.execution(), node, childGraph, inputs
            );

            if (result.status() == SubgraphStatus.COMPLETED) {
                return new NodeCompleted(result.outputs());
            } else if (result.status() == SubgraphStatus.SUBMITTED || result.status() == SubgraphStatus.RUNNING) {
                return new NodeWaiting("Subgraph execution in progress: " + result.childExecutionId().value());
            } else {
                return new NodeFailed("SUBGRAPH_FAILED",
                        "Subgraph execution failed: " + result.childExecutionId().value(), false);
            }
        } catch (Exception e) {
            return new NodeFailed("SUBGRAPH_ERROR", e.getMessage(), true);
        }
    }
}
