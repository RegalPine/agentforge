package io.github.regalpine.agentforge.skill.governance;

/**
 * Skill 风险等级.
 * AF-SKILL-03 §5: SkillRiskLevel.
 * AF-AD-15 §8.2: LOW/MEDIUM/HIGH/CRITICAL.
 *
 * <p>Determines the security controls
 * applied to a Skill. CRITICAL Skills
 * require human approval.
 */
public enum SkillRiskLevel {

    LOW(0),

    MEDIUM(1),

    HIGH(2),

    CRITICAL(3);

    private final int severity;

    SkillRiskLevel(int severity) {
        this.severity = severity;
    }

    public int severity() {
        return severity;
    }

    /**
     * Check if this risk level requires
     * human approval.
     */
    public boolean requiresApproval() {
        return this == CRITICAL;
    }

    /**
     * Check if this risk exceeds the
     * given maximum.
     */
    public boolean exceeds(SkillRiskLevel max) {
        return this.severity > max.severity;
    }
}
