package io.github.regalpine.agentforge.provider.mcp;

import java.util.Map;
import java.util.Objects;

/**
 * MCP resource definition.
 * AF-ENHANCE-10 §16: Resource.
 */
public record McpResourceDefinition(
    String uri,
    String name,
    String description,
    String mimeType,
    Map<String, Object> metadata
) {

    public McpResourceDefinition {
        Objects.requireNonNull(
            uri, "uri"
        );
        Objects.requireNonNull(
            name, "name"
        );

        description = description == null
            ? "" : description;
        mimeType = mimeType == null
            ? "application/octet-stream"
            : mimeType;
        metadata = metadata == null
            ? Map.of() : Map.copyOf(metadata);
    }
}
