package io.github.regalpine.agentforge.multiagent.team;

import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * AgentTeam — AF-ENHANCE-08 §19.
 *
 * A collaboration topology of
 * agent instances working together
 * on a shared goal.
 *
 * <p>Team is NOT a new Agent type.
 * It is a collaboration relationship.
 */
public record AgentTeam(

    TeamId id,

    String name,

    List<String> members,

    CollaborationTopology topology,

    TeamPolicy policy,

    Map<String, Object> metadata

) {

    public AgentTeam {
        Objects.requireNonNull(
            id, "id"
        );
        Objects.requireNonNull(
            topology, "topology"
        );

        members = members == null
            ? List.of()
            : List.copyOf(members);

        if (members.isEmpty()) {
            throw new IllegalArgumentException(
                "Team must have members"
            );
        }

        policy = policy == null
            ? TeamPolicy.defaults()
            : policy;

        metadata = metadata == null
            ? Map.of() : Map.copyOf(metadata);
    }

    /**
     * Create a team.
     */
    public static AgentTeam create(
        String name,
        List<String> members,
        CollaborationTopology topology
    ) {
        return new AgentTeam(
            TeamId.generate(),
            name, members, topology,
            null, null
        );
    }

    /**
     * Create with policy.
     */
    public static AgentTeam create(
        String name,
        List<String> members,
        CollaborationTopology topology,
        TeamPolicy policy
    ) {
        return new AgentTeam(
            TeamId.generate(),
            name, members, topology,
            policy, null
        );
    }

    /**
     * Get member count.
     */
    public int memberCount() {
        return members.size();
    }

    /**
     * Check if an agent is a member.
     */
    public boolean hasMember(
        String agentId
    ) {
        return members.contains(agentId);
    }
}
