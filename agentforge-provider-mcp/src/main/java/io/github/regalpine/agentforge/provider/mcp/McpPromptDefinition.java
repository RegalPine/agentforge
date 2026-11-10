package io.github.regalpine.agentforge.provider.mcp;

import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * MCP prompt definition.
 * AF-ENHANCE-10 §17: Prompt.
 */
public record McpPromptDefinition(
    String name,
    String description,
    List<McpPromptArgument> arguments,
    Map<String, Object> metadata
) {

    public McpPromptDefinition {
        Objects.requireNonNull(
            name, "name"
        );

        description = description == null
            ? "" : description;
        arguments = arguments == null
            ? List.of() : List.copyOf(arguments);
        metadata = metadata == null
            ? Map.of() : Map.copyOf(metadata);
    }

    /**
     * Prompt argument definition.
     */
    public record McpPromptArgument(
        String name,
        String description,
        boolean required
    ) {

        public McpPromptArgument {
            Objects.requireNonNull(
                name, "name"
            );
            description = description == null
                ? "" : description;
        }
    }
}
