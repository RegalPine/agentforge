package io.github.regalpine.agentforge.multiagent.team;

import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * TeamPolicy — AF-ENHANCE-08 §58-60.
 *
 * Policy for team execution:
 * completion criteria, concurrency,
 * required/optional members,
 * budget, timeout.
 */
public record TeamPolicy(

    CompletionCriteria completion,

    int quorumSize,

    int maxConcurrency,

    List<String> requiredAgents,

    List<String> optionalAgents,

    long timeoutMillis,

    int maxRetries,

    boolean propagateCancellation

) {

    public TeamPolicy {
        Objects.requireNonNull(
            completion, "completion"
        );

        requiredAgents = requiredAgents == null
            ? List.of()
            : List.copyOf(requiredAgents);

        optionalAgents = optionalAgents == null
            ? List.of()
            : List.copyOf(optionalAgents);

        if (quorumSize <= 0) {
            quorumSize = 1;
        }
        if (maxConcurrency <= 0) {
            maxConcurrency = 4;
        }
        if (timeoutMillis <= 0) {
            timeoutMillis = 300_000L;
        }
        if (maxRetries < 0) {
            maxRetries = 0;
        }
    }

    /**
     * Default team policy.
     */
    public static TeamPolicy defaults() {
        return new TeamPolicy(
            CompletionCriteria.ALL,
            1, 4, null, null,
            300_000L, 0, true
        );
    }

    /**
     * Permissive team policy.
     */
    public static TeamPolicy permissive() {
        return new TeamPolicy(
            CompletionCriteria.ANY,
            1, 16, null, null,
            600_000L, 2, false
        );
    }

    /**
     * Check if an agent is required.
     */
    public boolean isRequired(
        String agentId
    ) {
        return requiredAgents.contains(
            agentId
        );
    }

    /**
     * Check if an agent is optional.
     */
    public boolean isOptional(
        String agentId
    ) {
        return optionalAgents.contains(
            agentId
        );
    }
}
