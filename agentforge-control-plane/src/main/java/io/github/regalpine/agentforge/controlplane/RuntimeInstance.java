package io.github.regalpine.agentforge.controlplane;

/**
 * Runtime instance interface.
 * AF-ENHANCE-14 §11: Runtime Instance.
 *
 * <p>Runtime Instance = Deployment →
 * actual running unit. Not a new
 * Agent type.
 */
public interface RuntimeInstance {

    /**
     * Instance identity.
     */
    String id();

    /**
     * Parent deployment.
     */
    DeploymentId deployment();

    /**
     * Active revision.
     */
    String revisionId();

    /**
     * Instance status.
     */
    InstanceStatus status();

    /**
     * Instance health.
     */
    RuntimeHealth health();
}
