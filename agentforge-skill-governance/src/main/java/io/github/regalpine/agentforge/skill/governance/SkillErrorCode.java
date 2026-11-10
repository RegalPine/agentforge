package io.github.regalpine.agentforge.skill.governance;

/**
 * Skill 错误码枚举.
 * AF-AD-15 §10.4: 失败模型.
 *
 * <p>Unified error codes for Skill execution
 * failures. These codes are used in Execution
 * results and audit records.
 */
public enum SkillErrorCode {

    SKILL_NOT_FOUND,

    SKILL_VERSION_NOT_FOUND,

    SKILL_INPUT_INVALID,

    SKILL_DEPENDENCY_UNAVAILABLE,

    SKILL_POLICY_DENIED,

    SKILL_APPROVAL_REQUIRED,

    SKILL_TIMEOUT,

    SKILL_CANCELLED,

    SKILL_CYCLE_DETECTED,

    SKILL_BUDGET_EXCEEDED,

    SKILL_OUTPUT_INVALID,

    SKILL_EXECUTION_FAILED
}
