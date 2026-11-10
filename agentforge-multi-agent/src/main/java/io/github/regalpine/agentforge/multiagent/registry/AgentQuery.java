package io.github.regalpine.agentforge.multiagent.registry;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * AgentQuery — AF-ENHANCE-08 §24.
 *
 * Query for finding agents in
 * the registry by capabilities,
 * skills, or metadata.
 */
public record AgentQuery(

    Set<String> requiredCapabilities,

    Set<String> requiredSkills,

    String versionConstraint,

    Map<String, String> metadataFilter,

    int maxResults

) {

    public AgentQuery {
        requiredCapabilities =
            requiredCapabilities == null
                ? Set.of()
                : Set.copyOf(requiredCapabilities);

        requiredSkills =
            requiredSkills == null
                ? Set.of()
                : Set.copyOf(requiredSkills);

        metadataFilter =
            metadataFilter == null
                ? Map.of()
                : Map.copyOf(metadataFilter);

        if (maxResults <= 0) {
            maxResults = 10;
        }
    }

    /**
     * Query by capability.
     */
    public static AgentQuery byCapability(
        String capability
    ) {
        return new AgentQuery(
            Set.of(capability),
            null, null, null, 10
        );
    }

    /**
     * Query by skill.
     */
    public static AgentQuery bySkill(
        String skill
    ) {
        return new AgentQuery(
            null, Set.of(skill),
            null, null, 10
        );
    }

    /**
     * Empty query (find all).
     */
    public static AgentQuery all() {
        return new AgentQuery(
            null, null, null, null, 100
        );
    }
}
