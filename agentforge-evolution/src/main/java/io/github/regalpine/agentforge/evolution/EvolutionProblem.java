package io.github.regalpine.agentforge.evolution;

import java.util.List;

/**
 * Evolution problem definition.
 * AF-ENHANCE-15 §25: Problem
 * Definition.
 *
 * <p>A problem is identified from
 * production evidence and drives
 * candidate generation.
 */
public record EvolutionProblem(
    String problemId,
    ProblemCategory category,
    ProblemSeverity severity,
    List<EvidenceReference> evidence,
    String description
) {

    public EvolutionProblem {
        if (problemId == null
            || problemId.isBlank()) {
            throw new IllegalArgumentException(
                "problemId required");
        }
        if (category == null) {
            throw new IllegalArgumentException(
                "category required");
        }
        if (severity == null) {
            throw new IllegalArgumentException(
                "severity required");
        }
        evidence = evidence == null
            ? List.of()
            : List.copyOf(evidence);
    }

    /**
     * Create a problem.
     */
    public static EvolutionProblem of(
        String problemId,
        ProblemCategory category,
        ProblemSeverity severity,
        String description
    ) {
        return new EvolutionProblem(
            problemId, category,
            severity,
            List.of(), description
        );
    }

    /**
     * Add evidence to problem.
     */
    public EvolutionProblem withEvidence(
        EvidenceReference ref
    ) {
        var updated = new java.util.ArrayList<>(
            evidence);
        updated.add(ref);
        return new EvolutionProblem(
            problemId, category,
            severity,
            List.copyOf(updated),
            description
        );
    }
}
