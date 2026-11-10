package io.github.regalpine.agentforge.multiagent.team;


import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * TeamExecution — AF-ENHANCE-08 §55.
 *
 * Represents a running team execution.
 * Tracks member executions,
 * status, and results.
 */
public record TeamExecution(

    String executionId,

    TeamId teamId,

    String parentExecutionId,

    List<String> memberExecutionIds,

    TeamExecutionStatus status,

    Instant startedAt,

    Instant completedAt,

    Map<String, Object> teamState,

    Map<String, Object> results

) {

    public TeamExecution {
        Objects.requireNonNull(
            executionId, "executionId"
        );
        Objects.requireNonNull(
            teamId, "teamId"
        );

        memberExecutionIds =
            memberExecutionIds == null
                ? List.of()
                : List.copyOf(memberExecutionIds);

        status = status == null
            ? TeamExecutionStatus.CREATED
            : status;

        startedAt = startedAt == null
            ? Instant.now() : startedAt;

        teamState = teamState == null
            ? Map.of() : Map.copyOf(teamState);

        results = results == null
            ? Map.of() : Map.copyOf(results);
    }

    /**
     * Create a new team execution.
     */
    public static TeamExecution create(
        TeamId teamId,
        String parentExecutionId
    ) {
        return new TeamExecution(
            "team-exec-"
                + teamId.value(),
            teamId,
            parentExecutionId,
            List.of(),
            TeamExecutionStatus.CREATED,
            Instant.now(), null,
            Map.of(), Map.of()
        );
    }

    /**
     * Transition to new status.
     */
    public TeamExecution withStatus(
        TeamExecutionStatus newStatus
    ) {
        return new TeamExecution(
            executionId, teamId,
            parentExecutionId,
            memberExecutionIds,
            newStatus, startedAt,
            newStatus.isTerminal()
                ? Instant.now() : completedAt,
            teamState, results
        );
    }

    /**
     * Add a member execution.
     */
    public TeamExecution addMember(
        String memberExecId
    ) {
        var newList = new ArrayList<>(
            memberExecutionIds
        );
        newList.add(memberExecId);

        return new TeamExecution(
            executionId, teamId,
            parentExecutionId,
            List.copyOf(newList),
            status, startedAt,
            completedAt, teamState, results
        );
    }

    /**
     * Check if execution is done.
     */
    public boolean isComplete() {
        return status.isTerminal();
    }
}
