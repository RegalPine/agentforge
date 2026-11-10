package io.github.regalpine.agentforge.multiagent.reference;

import io.github.regalpine.agentforge.kernel.Reference;
import io.github.regalpine.agentforge.multiagent.contract.AgentContract;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * InMemoryAgentResolver — AE-11 §12-13.
 *
 * Simple in-memory resolver that stores contracts
 * keyed by "agentId:version". Supports exact version
 * match and wildcard (latest) resolution.
 */
public final class InMemoryAgentResolver
    implements AgentResolver {

    private final Map<String, AgentContract> contracts
        = new ConcurrentHashMap<>();

    @Override
    public AgentContract resolve(Reference reference) {
        String key = reference.id().value()
            + ":" + reference.version().value();

        AgentContract contract = contracts.get(key);
        if (contract == null) {
            throw new AgentResolutionException(
                reference.id().value(),
                reference.version().value()
            );
        }
        return contract;
    }

    @Override
    public void register(AgentContract contract) {
        String key = contract.agentId().value()
            + ":" + contract.version().value();
        contracts.put(key, contract);
    }

    /**
     * Get all registered contract keys.
     */
    public java.util.Set<String> registeredKeys() {
        return java.util.Set.copyOf(contracts.keySet());
    }

    /**
     * Number of registered contracts.
     */
    public int size() {
        return contracts.size();
    }
}
