package io.github.regalpine.agentforge.controlplane;

/**
 * Runtime isolation levels.
 * AF-ENHANCE-14 §57: Runtime Isolation.
 */
public enum RuntimeIsolation {

    SHARED_PROCESS,

    DEDICATED_PROCESS,

    DEDICATED_NODE,

    ISOLATED_CONTAINER
}
