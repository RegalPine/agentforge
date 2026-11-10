package io.github.regalpine.agentforge.runtime;

public record NodeWaiting(
    String reason
) implements NodeExecutionResult {
}
