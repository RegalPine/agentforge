package io.github.regalpine.agentforge.capability;

import io.github.regalpine.agentforge.kernel.EntityId;
import io.github.regalpine.agentforge.kernel.Schema;
import io.github.regalpine.agentforge.kernel.Version;

public record CapabilityDefinition(
    EntityId id,
    Version version,
    String name,
    Schema inputSchema,
    Schema outputSchema,
    CapabilityContract contract
) implements Capability {

    public CapabilityDefinition {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException(
                "Capability name cannot be blank"
            );
        }

        if (contract == null) {
            contract = CapabilityContract.defaults();
        }
    }
}
