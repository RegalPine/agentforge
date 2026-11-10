package io.github.regalpine.agentforge.agent;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Objects;
import java.util.Optional;

/**
 * Detects when the Agent is making no progress (stagnation).
 * <p>
 * Stagnation occurs when the Agent produces identical or near-identical
 * outputs for multiple consecutive iterations. This is a common failure
 * mode in LLM-based agents that loop without making progress.
 * <p>
 * The detector uses a sliding window of recent decisions and checks
 * for repeated patterns.
 */
public final class StagnationDetector {

    private final int windowSize;
    private final int threshold;
    private final Deque<String> decisionSignatures = new ArrayDeque<>();

    /**
     * Create a stagnation detector.
     *
     * @param windowSize number of recent decisions to examine
     * @param threshold  number of identical decisions to trigger stagnation
     */
    public StagnationDetector(int windowSize, int threshold) {
        if (windowSize < 2) {
            throw new IllegalArgumentException("windowSize must be >= 2");
        }
        if (threshold < 2) {
            throw new IllegalArgumentException("threshold must be >= 2");
        }
        if (threshold > windowSize) {
            throw new IllegalArgumentException("threshold must be <= windowSize");
        }
        this.windowSize = windowSize;
        this.threshold = threshold;
    }

    /**
     * Create a default stagnation detector (window=5, threshold=3).
     */
    public StagnationDetector() {
        this(5, 3);
    }

    /**
     * Record a decision signature and check for stagnation.
     *
     * @param signature a string representing the decision (e.g., tool call ID + arguments hash)
     * @return an Optional StopCondition if stagnation detected, empty otherwise
     */
    public Optional<StopCondition> record(String signature) {
        Objects.requireNonNull(signature, "signature cannot be null");

        decisionSignatures.addLast(signature);
        while (decisionSignatures.size() > windowSize) {
            decisionSignatures.removeFirst();
        }

        if (decisionSignatures.size() < threshold) {
            return Optional.empty();
        }

        // Check if the last `threshold` signatures are identical
        String[] sigs = decisionSignatures.toArray(new String[0]);
        String lastSig = sigs[sigs.length - 1];
        int consecutiveCount = 0;
        for (int i = sigs.length - 1; i >= 0; i--) {
            if (sigs[i].equals(lastSig)) {
                consecutiveCount++;
            } else {
                break;
            }
        }

        if (consecutiveCount >= threshold) {
            return Optional.of(StopCondition.of(
                StopCondition.StopReason.STAGNATION,
                "Agent made " + consecutiveCount + " identical decisions: " + lastSig
            ));
        }

        return Optional.empty();
    }

    /**
     * Reset the detector state.
     */
    public void reset() {
        decisionSignatures.clear();
    }

    /**
     * Get the current number of recorded signatures.
     *
     * @return the count
     */
    public int size() {
        return decisionSignatures.size();
    }

    public int getWindowSize() {
        return windowSize;
    }

    public int getThreshold() {
        return threshold;
    }
}
