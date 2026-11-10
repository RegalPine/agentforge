package io.github.regalpine.agentforge.workspace;

/**
 * Sandbox interface.
 * AF-ENHANCE-07 §31: Sandbox.
 *
 * <p>A restricted execution environment.
 * Sandbox mounts a Workspace and
 * enforces a SandboxPolicy.
 *
 * <p>Sandbox ≠ Workspace:
 * Workspace = resource space
 * Sandbox = execution environment
 */
public interface Sandbox {

    /**
     * Get sandbox identity.
     */
    SandboxId id();

    /**
     * Get sandbox status.
     */
    SandboxStatus status();

    /**
     * Get the mounted workspace.
     */
    Workspace workspace();

    /**
     * Get resource limits.
     */
    ResourceLimits limits();

    /**
     * Get sandbox policy.
     */
    SandboxPolicy policy();

    /**
     * Execute a process request.
     * Checks executable allowlist,
     * network policy, resource limits.
     */
    ProcessResult execute(
        ProcessRequest request
    );

    /**
     * Start the sandbox.
     */
    void start();

    /**
     * Stop the sandbox.
     */
    void stop();

    /**
     * Destroy the sandbox and
     * clean up resources.
     */
    void destroy();
}
