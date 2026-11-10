package io.github.regalpine.agentforge.model;

import java.util.Map;
import java.util.Objects;

/**
 * Describes a tool that can be invoked by the model.
 * <p>
 * A tool descriptor defines the tool's identity, input/output schema,
 * and other metadata. This is used by the model to understand what
 * tools are available and how to invoke them.
 */
public record ToolDescriptor(
    String id,
    String name,
    String description,
    Map<String, Object> inputSchema,
    Map<String, Object> outputSchema,
    Map<String, Object> metadata
) {

    public ToolDescriptor {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(name, "name");
        Objects.requireNonNull(description, "description");

        if (id.isBlank()) {
            throw new IllegalArgumentException("id cannot be blank");
        }
        if (name.isBlank()) {
            throw new IllegalArgumentException("name cannot be blank");
        }

        inputSchema = inputSchema == null ? Map.of() : Map.copyOf(inputSchema);
        outputSchema = outputSchema == null ? Map.of() : Map.copyOf(outputSchema);
        metadata = metadata == null ? Map.of() : Map.copyOf(metadata);
    }

    /**
     * Create a tool descriptor with the given parameters.
     *
     * @param id          unique identifier
     * @param name        display name
     * @param description description of what the tool does
     * @param inputSchema JSON schema for input
     * @return a new ToolDescriptor
     */
    public static ToolDescriptor of(
        String id,
        String name,
        String description,
        Map<String, Object> inputSchema
    ) {
        return new ToolDescriptor(id, name, description, inputSchema, Map.of(), Map.of());
    }

    /**
     * Create a tool descriptor with full parameters.
     *
     * @param id           unique identifier
     * @param name         display name
     * @param description  description of what the tool does
     * @param inputSchema  JSON schema for input
     * @param outputSchema JSON schema for output
     * @param metadata     additional metadata
     * @return a new ToolDescriptor
     */
    public static ToolDescriptor of(
        String id,
        String name,
        String description,
        Map<String, Object> inputSchema,
        Map<String, Object> outputSchema,
        Map<String, Object> metadata
    ) {
        return new ToolDescriptor(id, name, description, inputSchema, outputSchema, metadata);
    }
}
