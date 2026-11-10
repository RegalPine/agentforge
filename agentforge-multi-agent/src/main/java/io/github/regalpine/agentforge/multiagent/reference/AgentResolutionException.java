package io.github.regalpine.agentforge.multiagent.reference;

/**
 * Thrown when an agent reference cannot be resolved.
 */
public class AgentResolutionException extends RuntimeException {

    private final String agentId;

    public AgentResolutionException(String agentId) {
        super("Cannot resolve agent: " + agentId);
        this.agentId = agentId;
    }

    public AgentResolutionException(
        String agentId,
        String version
    ) {
        super("Cannot resolve agent: "
            + agentId + " v" + version);
        this.agentId = agentId;
    }

    public String agentId() {
        return agentId;
    }
}
