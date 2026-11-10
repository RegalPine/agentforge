package io.github.regalpine.agentforge.controlplane;

/**
 * Deployment interface.
 * AF-ENHANCE-14 §6: Deployment.
 *
 * <p>Deployment = production deployment
 * declaration for an Agent. Not a new
 * Agent type.
 */
public interface Deployment {

    /**
     * Deployment identity.
     */
    DeploymentId id();

    /**
     * Referenced agent.
     */
    String agentReference();

    /**
     * Target environment.
     */
    EnvironmentId environment();

    /**
     * Deployment specification.
     */
    DeploymentSpec spec();

    /**
     * Current status.
     */
    DeploymentStatus status();
}
