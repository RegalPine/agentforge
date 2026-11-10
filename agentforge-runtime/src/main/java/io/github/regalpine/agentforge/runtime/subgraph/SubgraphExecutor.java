package io.github.regalpine.agentforge.runtime.subgraph;

import io.github.regalpine.agentforge.event.Execution;
import io.github.regalpine.agentforge.event.ExecutionId;

import io.github.regalpine.agentforge.graph.GraphDefinition;
import io.github.regalpine.agentforge.graph.ir.ExecutionNode;
import io.github.regalpine.agentforge.runtime.*;

import java.time.Instant;
import java.util.Map;

/**
 * Executes a child graph (subgraph) within the context of a parent execution.
 * The subgraph inherits the parent's execution state and produces a result
 * that feeds back into the parent graph.
 *
 * <p>G-03: SUBGRAPH sub-graph execution engine.
 */
public final class SubgraphExecutor {

    private final ExecutionManager executionManager;

    public SubgraphExecutor(ExecutionManager executionManager) {
        this.executionManager = executionManager;
    }

    /**
     * Execute a subgraph synchronously.
     * Creates a child execution, waits for completion, and returns the result.
     *
     * @param parentExecution the parent execution context
     * @param subgraphNode    the SUBGRAPH node being executed
     * @param childGraph      the graph definition to execute
     * @param inputs          input data for the subgraph
     * @return the subgraph execution result
     */
    public SubgraphResult execute(Execution parentExecution, ExecutionNode subgraphNode,
                                   GraphDefinition childGraph, Map<String, Object> inputs) {
        ExecutionId childExecId = new ExecutionId(
                parentExecution.id().value() + "/sub/" + subgraphNode.id().value()
        );

        ExecutionRequest request = new ExecutionRequest(
                childGraph.id(),
                childGraph.version(),
                inputs,
                null
        );

        ExecutionHandle handle = executionManager.submit(request);

        return new SubgraphResult(
                childExecId,
                subgraphNode.id(),
                SubgraphStatus.SUBMITTED,
                Map.of(),
                Instant.now()
        );
    }

    /**
     * Resolve a completed subgraph result back into the parent execution context.
     */
    public Map<String, Object> resolveResult(SubgraphResult result) {
        if (result.status() == SubgraphStatus.COMPLETED) {
            return result.outputs();
        }
        if (result.status() == SubgraphStatus.FAILED) {
            throw new SubgraphExecutionException(
                    "Subgraph execution failed: " + result.childExecutionId().value(),
                    result.childExecutionId()
            );
        }
        throw new SubgraphExecutionException(
                "Subgraph execution not yet complete: " + result.status(),
                result.childExecutionId()
        );
    }
}
