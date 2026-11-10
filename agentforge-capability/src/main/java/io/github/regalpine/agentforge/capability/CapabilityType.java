package io.github.regalpine.agentforge.capability;

/**
 * Capability type classification.
 * AE-02 §13: Capability 分类体系.
 *
 * <p>Every capability in AgentForge is classified
 * into exactly one type. The type determines how
 * the invocation pipeline treats the capability.
 */
public enum CapabilityType {

    /**
     * Model invocation (LLM, embedding, etc.).
     */
    MODEL,

    /**
     * Tool invocation面向 Agent 调用的工具.
     */
    TOOL,

    /**
     * Agent delegation (sub-agent).
     */
    AGENT,

    /**
     * Retrieval (RAG, search).
     */
    RETRIEVAL,

    /**
     * Memory read/write.
     */
    MEMORY,

    /**
     * Human-in-the-loop.
     */
    HUMAN
}
