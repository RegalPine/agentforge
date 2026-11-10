package io.github.regalpine.agentforge.memory;

/**
 * Memory scope.
 * AE-08 §19: Memory Scope.
 *
 * <p>Determines which boundary a memory
 * belongs to. Memory must never leak
 * across scope boundaries without
 * explicit policy authorization.
 */
public enum MemoryScope {

    EXECUTION,

    CONVERSATION,

    USER,

    AGENT,

    TENANT
}
