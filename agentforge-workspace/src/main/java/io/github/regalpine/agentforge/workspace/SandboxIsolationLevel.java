package io.github.regalpine.agentforge.workspace;

/**
 * Sandbox isolation level.
 * AF-ENHANCE-07 §28: Sandbox
 * Isolation Levels.
 *
 * <p>V1.0: LOGICAL first,
 * then pluggable PROCESS,
 * CONTAINER, VM.
 */
public enum SandboxIsolationLevel {

    LOGICAL,

    PROCESS,

    CONTAINER,

    VM
}
