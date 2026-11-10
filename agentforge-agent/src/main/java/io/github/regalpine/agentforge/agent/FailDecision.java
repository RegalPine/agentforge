package io.github.regalpine.agentforge.agent;

/**
 * Decision to fail the Agent execution.
 * AF-06 §20: FailDecision.
 */
public record FailDecision(
    String code,
    String message
) implements AgentDecision {
}
