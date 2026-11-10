package io.github.regalpine.agentforge.evolution;

import java.util.List;

/**
 * Evolution candidate interface.
 * AF-ENHANCE-15 §13: Evolution
 * Candidate.
 *
 * <p>All improvements must first
 * become a Candidate. Candidates
 * are immutable once created.
 */
public interface EvolutionCandidate {

    /**
     * Candidate identity.
     */
    CandidateId id();

    /**
     * Base agent reference.
     */
    String baseAgent();

    /**
     * Change set.
     */
    CandidateChangeSet changes();

    /**
     * Supporting evidence.
     */
    List<EvidenceReference> evidence();

    /**
     * Current status.
     */
    CandidateStatus status();
}
