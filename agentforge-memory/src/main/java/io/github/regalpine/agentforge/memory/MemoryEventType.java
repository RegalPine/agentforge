package io.github.regalpine.agentforge.memory;

/**
 * Memory event types.
 * AE-08 §67: Memory Events.
 */
public enum MemoryEventType {

    MEMORY_CANDIDATE_CREATED,

    MEMORY_WRITE_STARTED,

    MEMORY_WRITE_COMPLETED,

    MEMORY_WRITE_REJECTED,

    MEMORY_RETRIEVAL_STARTED,

    MEMORY_RETRIEVAL_COMPLETED,

    MEMORY_ACCESS_DENIED,

    MEMORY_EXPIRED,

    MEMORY_DELETED,

    CONTEXT_BUILD_STARTED,

    CONTEXT_BUILD_COMPLETED
}
