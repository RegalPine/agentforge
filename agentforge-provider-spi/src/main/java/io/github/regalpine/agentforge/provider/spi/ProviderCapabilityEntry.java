package io.github.regalpine.agentforge.provider.spi;

import io.github.regalpine.agentforge.kernel.EntityId;
import io.github.regalpine.agentforge.kernel.Version;

public record ProviderCapabilityEntry(
    EntityId capabilityId,
    Version capabilityVersion
) {
}
