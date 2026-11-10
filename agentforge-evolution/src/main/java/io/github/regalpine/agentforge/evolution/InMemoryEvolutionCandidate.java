package io.github.regalpine.agentforge.evolution;

import java.util.List;
import java.util.Map;

/**
 * In-memory EvolutionCandidate.
 * AF-ENHANCE-15 §13: Evolution
 * Candidate.
 */
public record InMemoryEvolutionCandidate(
    CandidateId candidateId,
    String baseAgentRef,
    CandidateChangeSet changeSet,
    List<EvidenceReference> evidenceList,
    CandidateStatus candidateStatus,
    String problemId,
    String digest,
    Map<String, String> metadata
) implements EvolutionCandidate {

    public InMemoryEvolutionCandidate {
        if (candidateId == null) {
            throw new IllegalArgumentException(
                "candidateId required");
        }
        if (baseAgentRef == null
            || baseAgentRef.isBlank()) {
            throw new IllegalArgumentException(
                "baseAgent required");
        }
        if (changeSet == null) {
            throw new IllegalArgumentException(
                "changes required");
        }
        evidenceList = evidenceList == null
            ? List.of()
            : List.copyOf(evidenceList);
        if (candidateStatus == null) {
            candidateStatus =
                CandidateStatus.CREATED;
        }
        metadata = metadata == null
            ? Map.of() : Map.copyOf(metadata);
    }

    @Override
    public CandidateId id() {
        return candidateId;
    }

    @Override
    public String baseAgent() {
        return baseAgentRef;
    }

    @Override
    public CandidateChangeSet changes() {
        return changeSet;
    }

    @Override
    public List<EvidenceReference> evidence() {
        return evidenceList;
    }

    @Override
    public CandidateStatus status() {
        return candidateStatus;
    }

    /**
     * Create a new candidate.
     */
    public static InMemoryEvolutionCandidate of(
        CandidateId id,
        String baseAgent,
        CandidateChangeSet changes,
        List<EvidenceReference> evidence
    ) {
        return new InMemoryEvolutionCandidate(
            id, baseAgent, changes,
            evidence,
            CandidateStatus.CREATED,
            null, null, Map.of()
        );
    }

    /**
     * Transition to validating.
     */
    public InMemoryEvolutionCandidate validate() {
        return new InMemoryEvolutionCandidate(
            candidateId, baseAgentRef,
            changeSet, evidenceList,
            CandidateStatus.VALIDATING,
            problemId, digest, metadata
        );
    }

    /**
     * Transition to experimenting.
     */
    public InMemoryEvolutionCandidate experiment() {
        return new InMemoryEvolutionCandidate(
            candidateId, baseAgentRef,
            changeSet, evidenceList,
            CandidateStatus.EXPERIMENTING,
            problemId, digest, metadata
        );
    }

    /**
     * Approve this candidate.
     */
    public InMemoryEvolutionCandidate approve() {
        return new InMemoryEvolutionCandidate(
            candidateId, baseAgentRef,
            changeSet, evidenceList,
            CandidateStatus.APPROVED,
            problemId, digest, metadata
        );
    }

    /**
     * Reject this candidate.
     */
    public InMemoryEvolutionCandidate reject() {
        return new InMemoryEvolutionCandidate(
            candidateId, baseAgentRef,
            changeSet, evidenceList,
            CandidateStatus.REJECTED,
            problemId, digest, metadata
        );
    }

    /**
     * Release this candidate.
     */
    public InMemoryEvolutionCandidate release() {
        return new InMemoryEvolutionCandidate(
            candidateId, baseAgentRef,
            changeSet, evidenceList,
            CandidateStatus.RELEASED,
            problemId, digest, metadata
        );
    }

    /**
     * Add evidence.
     */
    public InMemoryEvolutionCandidate addEvidence(
        EvidenceReference ref
    ) {
        var updated = new java.util.ArrayList<>(
            evidenceList);
        updated.add(ref);
        return new InMemoryEvolutionCandidate(
            candidateId, baseAgentRef,
            changeSet,
            List.copyOf(updated),
            candidateStatus,
            problemId, digest, metadata
        );
    }
}
