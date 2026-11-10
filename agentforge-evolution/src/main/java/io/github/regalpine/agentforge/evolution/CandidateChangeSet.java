package io.github.regalpine.agentforge.evolution;

import java.util.List;

/**
 * Candidate change set.
 * AF-ENHANCE-15 §15: Candidate
 * ChangeSet.
 */
public record CandidateChangeSet(
    List<Change> changes,
    ChangeScope scope,
    String digest
) {

    public CandidateChangeSet {
        if (changes == null
            || changes.isEmpty()) {
            throw new IllegalArgumentException(
                "changes required");
        }
        if (scope == null) {
            throw new IllegalArgumentException(
                "scope required");
        }
        changes = List.copyOf(changes);
    }

    /**
     * Create simple change set.
     */
    public static CandidateChangeSet of(
        List<Change> changes,
        ChangeScope scope
    ) {
        return new CandidateChangeSet(
            changes, scope, null);
    }

    /**
     * Number of changes.
     */
    public int size() {
        return changes.size();
    }
}
