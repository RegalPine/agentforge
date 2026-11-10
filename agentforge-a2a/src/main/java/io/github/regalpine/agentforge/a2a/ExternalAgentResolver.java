package io.github.regalpine.agentforge.a2a;

import java.util.List;

/**
 * External agent resolver.
 * AF-ENHANCE-10 §50: Resolver.
 *
 * <p>Resolves external agents
 * based on capability queries
 * and resolution context.
 */
public interface ExternalAgentResolver {

    /**
     * Resolve agents matching
     * the capability query.
     */
    List<AgentCard> resolve(
        AgentCapabilityQuery query
    );

    /**
     * Resolve a specific agent
     * by reference.
     */
    AgentCard resolve(
        ExternalAgentReference reference
    );
}
