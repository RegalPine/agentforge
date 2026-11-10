package io.github.regalpine.agentforge.memory;

/**
 * Context item priority.
 * AF-ENHANCE-06 §40: Context Priority.
 *
 * <p>When budget is insufficient,
 * items are removed lowest-first:
 * LOW → NORMAL → HIGH.
 * CRITICAL must never be silently
 * removed.
 */
public enum ContextPriority {

    CRITICAL,

    HIGH,

    NORMAL,

    LOW
}
