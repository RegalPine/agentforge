package io.github.regalpine.agentforge.agent;

import java.time.Instant;
import java.util.Map;

/**
 * Memory item for Agent Memory storage.
 * AF-06 §47: MemoryItem.
 *
 * <p>Represents a unit of long-term or
 * cross-execution information that Agent
 * can store and retrieve.
 */
public record MemoryItem(
    String id,
    Object content,
    Map<String, Object> metadata,
    Instant createdAt
) {

    public MemoryItem {
        metadata =
            metadata == null
                ? Map.of()
                : Map.copyOf(metadata);
    }
}
