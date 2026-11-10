package io.github.regalpine.agentforge.runtime;

public record NodeCancelled(
    String reason
) implements NodeExecutionResult {
}
