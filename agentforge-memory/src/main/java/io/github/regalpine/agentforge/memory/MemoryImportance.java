package io.github.regalpine.agentforge.memory;

/**
 * Memory importance level.
 * AE-08 §33: Memory Importance.
 *
 * <p>Used for ranking during retrieval.
 * User-explicit memories may carry
 * higher importance.
 */
public enum MemoryImportance {

    LOW(0),

    NORMAL(1),

    HIGH(2),

    CRITICAL(3);

    private final int weight;

    MemoryImportance(int weight) {
        this.weight = weight;
    }

    public int weight() {
        return weight;
    }
}
