package io.github.regalpine.agentforge.skill.governance;

import java.util.Objects;
import java.util.Set;

/**
 * Skill 数据边界.
 * AF-SKILL-03 §16-§17: SkillDataBoundary.
 * AF-AD-15 §8.3: Data Boundary.
 *
 * <p>Defines the data flow constraints
 * for a Skill. Controls what data
 * classifications can flow in/out
 * and which external endpoints are
 * permitted.
 *
 * <p>Immutable. All sets are defensively
 * copied.
 */
public record SkillDataBoundary(
    Set<String> permittedEgressHosts,
    boolean allowsExternalNetwork,
    boolean allowsFileSystemAccess,
    boolean allowsCredentialAccess
) {

    public SkillDataBoundary {
        permittedEgressHosts =
            permittedEgressHosts == null
                ? Set.of()
                : Set.copyOf(permittedEgressHosts);
    }

    /**
     * Default restrictive boundary:
     * no external access.
     */
    public static SkillDataBoundary restrictive() {
        return new SkillDataBoundary(
            Set.of(), false, false, false
        );
    }

    /**
     * Check if a host is permitted for
     * egress.
     */
    public boolean isEgressAllowed(String host) {
        if (!allowsExternalNetwork) {
            return false;
        }
        return permittedEgressHosts.contains(
            Objects.requireNonNull(host)
        );
    }
}
