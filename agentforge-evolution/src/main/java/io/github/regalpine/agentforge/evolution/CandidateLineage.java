package io.github.regalpine.agentforge.evolution;

import java.util.List;

/**
 * Candidate lineage tracking.
 * AF-ENHANCE-15 §53: Candidate
 * Lineage.
 */
public record CandidateLineage(
    CandidateId candidateId,
    String baseAgent,
    CandidateId parentCandidate,
    List<EvidenceReference> evidence
) {

    public CandidateLineage {
        if (candidateId == null) {
            throw new IllegalArgumentException(
                "candidateId required");
        }
        if (baseAgent == null
            || baseAgent.isBlank()) {
            throw new IllegalArgumentException(
                "baseAgent required");
        }
        evidence = evidence == null
            ? List.of()
            : List.copyOf(evidence);
    }

    /**
     * Create root lineage (no parent).
     */
    public static CandidateLineage root(
        CandidateId candidateId,
        String baseAgent
    ) {
        return new CandidateLineage(
            candidateId, baseAgent,
            null, List.of()
        );
    }

    /**
     * Check if this has a parent.
     */
    public boolean hasParent() {
        return parentCandidate != null;
    }
}
