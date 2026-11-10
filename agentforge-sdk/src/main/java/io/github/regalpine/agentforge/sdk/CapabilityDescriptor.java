package io.github.regalpine.agentforge.sdk;

import java.util.Map;

/**
 * Read-only descriptor for a Capability.
 */
public record CapabilityDescriptor(
    String id,
    String version,
    String description,
    Map<String, Object> inputSchema,
    Map<String, Object> outputSchema
) {

    public CapabilityDescriptor {
        inputSchema = inputSchema == null ? Map.of() : Map.copyOf(inputSchema);
        outputSchema = outputSchema == null ? Map.of() : Map.copyOf(outputSchema);
    }
}
