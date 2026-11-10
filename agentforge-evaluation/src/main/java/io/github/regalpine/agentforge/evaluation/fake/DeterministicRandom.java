package io.github.regalpine.agentforge.evaluation.fake;

import java.util.Random;

/**
 * DeterministicRandom — AE-12 §64.
 *
 * A seeded Random that produces deterministic sequences for reproducible tests.
 */
public final class DeterministicRandom extends Random {

    private final long seed;

    public DeterministicRandom(long seed) {
        super(seed);
        this.seed = seed;
    }

    /**
     * Returns the seed used to initialize this random.
     */
    public long seedValue() {
        return seed;
    }

    /**
     * Creates a new DeterministicRandom with the same seed,
     * producing the same sequence from the beginning.
     */
    public DeterministicRandom reset() {
        return new DeterministicRandom(seed);
    }

    @Override
    public String toString() {
        return "DeterministicRandom{seed=" + seed + "}";
    }
}
