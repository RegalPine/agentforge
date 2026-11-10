package io.github.regalpine.agentforge.multiagent.reference;

import java.util.Objects;

/**
 * AgentReference — AF-ENHANCE-08 §27.
 *
 * A reference to a specific agent
 * with version and digest for
 * production-grade identification.
 *
 * <p>Agent ID + Version + Digest
 * ensures integrity and versioning.
 */
public record AgentReference(

    String agentId,

    String version,

    String digest

) {

    public AgentReference {
        Objects.requireNonNull(
            agentId, "agentId"
        );

        version = version == null
            ? "1.0.0" : version;
    }

    /**
     * Create a reference.
     */
    public static AgentReference of(
        String agentId,
        String version
    ) {
        return new AgentReference(
            agentId, version, null
        );
    }

    /**
     * Create with digest.
     */
    public static AgentReference of(
        String agentId,
        String version,
        String digest
    ) {
        return new AgentReference(
            agentId, version, digest
        );
    }

    /**
     * Create with just agent ID.
     */
    public static AgentReference of(
        String agentId
    ) {
        return new AgentReference(
            agentId, null, null
        );
    }
}
