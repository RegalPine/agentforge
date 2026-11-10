package io.github.regalpine.agentforge.agent;

/**
 * Agent decision sealed interface.
 * AF-06 §17: AgentDecision.
 *
 * <p>Output of the Decision Engine. The Agent
 * must choose exactly one of four decision types:
 * invoke a capability, finish, wait, or fail.
 */
public sealed interface AgentDecision
    permits CapabilityDecision,
            SkillDecision,
            FinishDecision,
            WaitDecision,
            FailDecision {
}
