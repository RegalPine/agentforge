package io.github.regalpine.agentforge.runtime;

import io.github.regalpine.agentforge.graph.ir.ExecutionNode;

public interface NodeExecutor {

    NodeExecutionResult execute(
        ExecutionNode node,
        NodeExecutionContext context
    );
}
