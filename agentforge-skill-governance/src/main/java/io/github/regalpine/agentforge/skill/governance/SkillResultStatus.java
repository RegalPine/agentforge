package io.github.regalpine.agentforge.skill.governance;

/**
 * Skill 调用结果状态.
 * AF-AD-15 §6.2: SkillResult status.
 *
 * <p>Possible outcomes of a Skill invocation.
 */
public enum SkillResultStatus {

    COMPLETED,

    FAILED,

    TIMEOUT,

    CANCELLED,

    WAITING_HUMAN
}
