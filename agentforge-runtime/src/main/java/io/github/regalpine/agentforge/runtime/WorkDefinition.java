package io.github.regalpine.agentforge.runtime;

import io.github.regalpine.agentforge.kernel.EntityId;
import io.github.regalpine.agentforge.kernel.Schema;
import io.github.regalpine.agentforge.kernel.Version;

/**
 * Work definition record.
 * AF-10 §12: WorkDefinition.
 *
 * <p>Represents a unit of executable work
 * with input/output schema definitions.
 */
public record WorkDefinition(
    EntityId id,
    Version version,
    String name,
    String description,
    Schema inputSchema,
    Schema outputSchema
) {

    public WorkDefinition {
        if (id == null) {
            throw new IllegalArgumentException("id cannot be null");
        }

        if (version == null) {
            throw new IllegalArgumentException("version cannot be null");
        }

        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("name cannot be blank");
        }
    }
}
