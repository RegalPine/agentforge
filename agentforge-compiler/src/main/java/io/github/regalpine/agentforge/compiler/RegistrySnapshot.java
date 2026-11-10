package io.github.regalpine.agentforge.compiler;

import io.github.regalpine.agentforge.agent.AgentDefinition;
import io.github.regalpine.agentforge.capability.CapabilityDefinition;
import io.github.regalpine.agentforge.capability.ProviderDescriptor;
import io.github.regalpine.agentforge.kernel.Reference;

import java.util.Map;

public record RegistrySnapshot(
    Map<Reference, AgentDefinition> agents,
    Map<Reference, CapabilityDefinition> capabilities,
    Map<Reference, ProviderDescriptor> providers
) {

    public RegistrySnapshot {
        agents =
            agents == null
                ? Map.of()
                : Map.copyOf(agents);

        capabilities =
            capabilities == null
                ? Map.of()
                : Map.copyOf(capabilities);

        providers =
            providers == null
                ? Map.of()
                : Map.copyOf(providers);
    }

    public static RegistrySnapshot empty() {
        return new RegistrySnapshot(
            Map.of(),
            Map.of(),
            Map.of()
        );
    }
}
