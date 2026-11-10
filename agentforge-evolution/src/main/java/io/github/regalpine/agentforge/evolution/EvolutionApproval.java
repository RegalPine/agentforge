package io.github.regalpine.agentforge.evolution;

import java.time.Instant;

/**
 * Evolution approval record.
 * AF-ENHANCE-15 §43: Approval.
 *
 * <p>Approval is a governance
 * decision. It does not modify
 * the Candidate.
 */
public record EvolutionApproval(
    String approvalId,
    CandidateId candidateId,
    String reviewer,
    ApprovalDecision decision,
    String reason,
    Instant timestamp
) {

    public EvolutionApproval {
        if (approvalId == null
            || approvalId.isBlank()) {
            throw new IllegalArgumentException(
                "approvalId required");
        }
        if (candidateId == null) {
            throw new IllegalArgumentException(
                "candidateId required");
        }
        if (reviewer == null
            || reviewer.isBlank()) {
            throw new IllegalArgumentException(
                "reviewer required");
        }
        if (decision == null) {
            throw new IllegalArgumentException(
                "decision required");
        }
        if (timestamp == null) {
            timestamp = Instant.now();
        }
    }

    /**
     * Create an approval.
     */
    public static EvolutionApproval of(
        String approvalId,
        CandidateId candidateId,
        String reviewer,
        ApprovalDecision decision,
        String reason
    ) {
        return new EvolutionApproval(
            approvalId, candidateId,
            reviewer, decision,
            reason, Instant.now()
        );
    }
}
