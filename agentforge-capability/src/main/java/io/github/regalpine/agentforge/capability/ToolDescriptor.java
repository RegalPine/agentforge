package io.github.regalpine.agentforge.capability;

import java.util.Map;

/**
 * Generic tool descriptor.
 * AF-07 §25: ToolDescriptor.
 *
 * <p>Protocol-agnostic description of a Tool
 * that can be discovered through a ToolRegistry.
 * Provider-specific implementations (e.g. McpToolDescriptor)
 * can extend this concept.
 */
public record ToolDescriptor(
    String toolId,
    String version,
    String protocol,
    Map<String, Object> metadata
) {

    public ToolDescriptor {
        if (toolId == null || toolId.isBlank()) {
            throw new IllegalArgumentException(
                "toolId cannot be blank"
            );
        }

        metadata =
            metadata == null
                ? Map.of()
                : Map.copyOf(metadata);
    }
}
