package io.github.regalpine.agentforge.controlplane;

/**
 * Deployment strategy types.
 * AF-ENHANCE-14 §39: Deployment Strategy.
 */
public enum DeploymentStrategy {

    ROLLING,

    BLUE_GREEN,

    CANARY,

    RECREATE
}
