package io.github.regalpine.agentforge.sdk;

/** Thrown when a capability invocation fails. */
public class CapabilityInvocationException extends AgentForgeException {
    public CapabilityInvocationException(String capabilityId, String message) {
        super("CAPABILITY_ERROR", message, null, null, null, capabilityId, false, java.util.Map.of());
    }

    public CapabilityInvocationException(String capabilityId, String message, Throwable cause) {
        super("CAPABILITY_ERROR", message, cause, null, null, capabilityId, false, java.util.Map.of());
    }
}
