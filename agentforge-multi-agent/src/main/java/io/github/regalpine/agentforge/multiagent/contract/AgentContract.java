package io.github.regalpine.agentforge.multiagent.contract;

import io.github.regalpine.agentforge.kernel.EntityId;
import io.github.regalpine.agentforge.kernel.Reference;
import io.github.regalpine.agentforge.kernel.Schema;
import io.github.regalpine.agentforge.kernel.Version;

import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * AgentContract — AE-11 §5.
 *
 * Defines the contract an agent exposes for multi-agent
 * collaboration: identity, version, input/output schemas,
 * execution policy, trust level, and declared capabilities.
 *
 * <p>When an agent is called by another agent, this contract
 * is the sole interface the caller depends on.
 */
public record AgentContract(

    EntityId agentId,

    Version version,

    Schema inputSchema,

    Schema outputSchema,

    AgentExecutionPolicy executionPolicy,

    AgentTrustLevel trustLevel,

    Set<Reference> capabilities,

    Map<String, String> metadata
) {

    public AgentContract {
        Objects.requireNonNull(agentId, "agentId");
        Objects.requireNonNull(version, "version");

        executionPolicy = executionPolicy == null
            ? AgentExecutionPolicy.defaults()
            : executionPolicy;

        trustLevel = trustLevel == null
            ? AgentTrustLevel.STANDARD
            : trustLevel;

        capabilities = capabilities == null
            ? Set.of()
            : Set.copyOf(capabilities);

        metadata = metadata == null
            ? Map.of()
            : Map.copyOf(metadata);
    }

    /**
     * Create a minimal contract with just id and version.
     */
    public static AgentContract of(
        String agentId,
        String version
    ) {
        return new AgentContract(
            new EntityId(agentId),
            new Version(version),
            null, null,
            null, null,
            null, null
        );
    }

    /**
     * Get the agent reference for this contract.
     */
    public Reference toReference() {
        return new Reference(agentId, version);
    }
}
