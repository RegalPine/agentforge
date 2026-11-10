package io.github.regalpine.agentforge.sdk;

/** Thrown when a policy denies an operation. */
public class PolicyDeniedException extends AgentForgeException {
    public PolicyDeniedException(String message) {
        super("POLICY_DENIED", message);
    }
}
