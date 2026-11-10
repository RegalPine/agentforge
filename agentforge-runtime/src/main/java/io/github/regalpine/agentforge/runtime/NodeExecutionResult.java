package io.github.regalpine.agentforge.runtime;

public sealed interface NodeExecutionResult
    permits NodeCompleted,
            NodeFailed,
            NodeWaiting,
            NodeCancelled,
            NodeSkipped {
}
