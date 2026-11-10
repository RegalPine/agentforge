package io.github.regalpine.agentforge.agent;

import io.github.regalpine.agentforge.kernel.Reference;

import java.util.List;
import java.util.Optional;

public interface AgentRegistry {

    void register(AgentDefinition agent);

    Optional<AgentDefinition> find(
        Reference reference
    );

    List<AgentDefinition> list();

    void unregister(Reference reference);
}
