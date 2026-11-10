package io.github.regalpine.agentforge.kernel;

/**
 * Resolved Skill with bound version.
 * AF-SKILL-01 §6: ResolvedSkill.
 * AF-AD-15 §6.2: SkillResolver 返回.
 *
 * <p>Returned by SkillResolver after
 * resolving a SkillReference against
 * the Registry. Contains the full Skill
 * definition with version bound.
 *
 * <p>Immutable.
 */
public record ResolvedSkill(
    Skill skill,
    SkillReference reference,
    SkillExecutionStrategy strategy
) {

    public ResolvedSkill {
        if (skill == null) {
            throw new IllegalArgumentException(
                "skill cannot be null"
            );
        }
        if (reference == null) {
            throw new IllegalArgumentException(
                "reference cannot be null"
            );
        }
        if (strategy == null) {
            throw new IllegalArgumentException(
                "strategy cannot be null"
            );
        }
    }
}
