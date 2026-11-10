package io.github.regalpine.agentforge.multiagent.reference;

import io.github.regalpine.agentforge.kernel.Reference;
import io.github.regalpine.agentforge.multiagent.contract.AgentContract;

/**
 * AgentResolver — AE-11 §12.
 *
 * Resolves an AgentReference to an AgentContract.
 * Agents should not depend on Java objects directly;
 * they use references and let the resolver find the
 * concrete agent version.
 */
public interface AgentResolver {

    /**
     * Resolve an agent reference to its contract.
     *
     * @param reference the agent reference (agentId + version)
     * @return the resolved contract
     * @throws AgentResolutionException if not found
     */
    AgentContract resolve(Reference reference);

    /**
     * Register a contract for resolution.
     */
    void register(AgentContract contract);
}
