package io.github.regalpine.agentforge.runtime;

/**
 * Skill 解析异常.
 * AF-AD-15 §10.4: SKILL_NOT_FOUND /
 * SKILL_VERSION_NOT_FOUND.
 *
 * <p>Thrown when a Skill cannot be
 * resolved from the Registry.
 */
public class SkillResolutionException
    extends RuntimeException {

    private final String skillId;

    public SkillResolutionException(
        String skillId,
        String message
    ) {
        super(message);
        this.skillId = skillId;
    }

    public SkillResolutionException(
        String skillId,
        String message,
        Throwable cause
    ) {
        super(message, cause);
        this.skillId = skillId;
    }

    /**
     * @return the Skill ID that failed
     *         to resolve.
     */
    public String skillId() {
        return skillId;
    }
}
