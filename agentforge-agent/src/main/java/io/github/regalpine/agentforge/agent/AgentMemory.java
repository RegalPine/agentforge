package io.github.regalpine.agentforge.agent;

import java.util.List;

/**
 * Agent Memory SPI.
 * AF-06 §47: AgentMemory.
 *
 * <p>Provides long-term or cross-execution
 * information storage for Agents. Distinct
 * from AgentContext which is per-execution.
 */
public interface AgentMemory {

    List<MemoryItem> retrieve(
        MemoryQuery query
    );

    void store(
        MemoryItem item
    );
}
