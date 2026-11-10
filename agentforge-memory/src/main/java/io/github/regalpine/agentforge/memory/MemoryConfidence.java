package io.github.regalpine.agentforge.memory;

/**
 * Memory confidence.
 * AE-08 §11: Memory Confidence.
 *
 * <p>Automatically produced memories
 * (AGENT_INFERRED) should carry lower
 * confidence than USER_EXPLICIT ones.
 */
public record MemoryConfidence(
    double score,
    ConfidenceBasis basis
) {

    public MemoryConfidence {
        if (score < 0.0 || score > 1.0) {
            throw new IllegalArgumentException(
                "Confidence score must be in [0.0, 1.0]"
            );
        }

        if (basis == null) {
            throw new IllegalArgumentException(
                "ConfidenceBasis cannot be null"
            );
        }
    }

    public static MemoryConfidence high(
        ConfidenceBasis basis
    ) {
        return new MemoryConfidence(0.9, basis);
    }

    public static MemoryConfidence medium(
        ConfidenceBasis basis
    ) {
        return new MemoryConfidence(0.6, basis);
    }

    public static MemoryConfidence low(
        ConfidenceBasis basis
    ) {
        return new MemoryConfidence(0.3, basis);
    }
}
