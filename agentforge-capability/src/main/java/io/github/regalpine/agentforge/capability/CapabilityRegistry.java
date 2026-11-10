package io.github.regalpine.agentforge.capability;

import io.github.regalpine.agentforge.kernel.Reference;

import java.util.List;
import java.util.Optional;

public interface CapabilityRegistry {

    void register(CapabilityDefinition capability);

    Optional<CapabilityDefinition> find(
        Reference reference
    );

    List<CapabilityDefinition> list();

    void unregister(Reference reference);
}
