package io.github.regalpine.agentforge.sdk;

/** Thrown when an agent/capability/graph cannot be resolved. */
public class ResolutionException extends AgentForgeException {
    public ResolutionException(String resourceType, String resourceId) {
        super("RESOLUTION_ERROR", resourceType + " not found: " + resourceId);
    }
}
