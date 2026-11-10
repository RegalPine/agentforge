package io.github.regalpine.agentforge.workspace;

/**
 * Sandbox status.
 * AF-ENHANCE-07 §31: Sandbox.
 */
public enum SandboxStatus {

    CREATED,

    STARTING,

    RUNNING,

    PAUSED,

    STOPPING,

    STOPPED,

    FAILED,

    DESTROYED
}
