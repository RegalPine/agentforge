package io.github.regalpine.agentforge.capability;

import io.github.regalpine.agentforge.kernel.Reference;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public final class InMemoryCapabilityRegistry
    implements CapabilityRegistry {

    private final Map<
        Reference,
        CapabilityDefinition
    > capabilities = new ConcurrentHashMap<>();

    @Override
    public void register(
        CapabilityDefinition capability
    ) {
        Reference reference =
            new Reference(
                capability.id(),
                capability.version()
            );

        CapabilityDefinition previous =
            capabilities.putIfAbsent(
                reference,
                capability
            );

        if (previous != null) {
            throw new IllegalStateException(
                "Capability already registered: "
                    + reference
            );
        }
    }

    @Override
    public Optional<CapabilityDefinition> find(
        Reference reference
    ) {
        return Optional.ofNullable(
            capabilities.get(reference)
        );
    }

    @Override
    public List<CapabilityDefinition> list() {
        return List.copyOf(
            new ArrayList<>(capabilities.values())
        );
    }

    @Override
    public void unregister(
        Reference reference
    ) {
        capabilities.remove(reference);
    }
}
