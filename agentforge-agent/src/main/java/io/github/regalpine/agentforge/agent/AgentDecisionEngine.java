package io.github.regalpine.agentforge.agent;

/**
 * Agent Decision Engine SPI.
 * AF-06 §15: AgentDecisionEngine.
 *
 * <p>Core extension point for Agent behavior.
 * AgentForge does not mandate LLM usage —
 * the Decision Engine can be a Rule Engine,
 * Deterministic Planner, LLM, Hybrid, or
 * any custom algorithm.
 */
public interface AgentDecisionEngine {

    AgentDecision decide(
        AgentDecisionContext context
    );
}
