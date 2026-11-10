package io.github.regalpine.agentforge.memory;

/**
 * Memory update strategy.
 * AE-08 §36: Memory Update.
 *
 * <p>MVP recommends VERSION to preserve
 * history traceability.
 */
public enum MemoryUpdateStrategy {

    REPLACE,

    MERGE,

    APPEND,

    VERSION
}
