package io.github.regalpine.agentforge.evolution;

/**
 * Candidate identity.
 * AF-ENHANCE-15 §13: Evolution
 * Candidate.
 */
public record CandidateId(
    String value
) {

    public CandidateId {
        if (value == null
            || value.isBlank()) {
            throw new IllegalArgumentException(
                "value required");
        }
    }
}
