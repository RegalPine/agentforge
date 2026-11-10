package io.github.regalpine.agentforge.memory;

/**
 * Memory type classification.
 * AE-08 §8: Memory 类型.
 *
 * <p>MVP types. Complex types (Semantic,
 * Episodic, Procedural, Reflective) can
 * be mapped to these basics.
 */
public enum MemoryType {

    CONVERSATION,

    FACT,

    PREFERENCE,

    TASK,

    EXPERIENCE,

    SUMMARY
}
