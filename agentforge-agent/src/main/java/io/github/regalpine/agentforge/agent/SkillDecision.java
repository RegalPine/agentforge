package io.github.regalpine.agentforge.agent;

import java.util.Map;

/**
 * Decision to invoke a Skill.
 * AF-SKILL-01 §27-§30: CALL_SKILL Action.
 * AF-AD-15 §7.1: Agent Loop CALL_SKILL.
 *
 * <p>Produced by the Decision Engine when
 * the LLM selects a Skill as the next
 * action. The Runtime converts this into
 * a SkillInvocationRequest and delegates
 * to SkillInvoker.
 *
 * <p>LLM ≠ Authorization Authority.
 * The actual execution still goes through
 * Policy + Capability Security.
 */
public record SkillDecision(
    DecisionType type,
    String skillId,
    String skillVersion,
    Map<String, Object> arguments,
    String reason
) implements AgentDecision {

    public SkillDecision {
        if (type != DecisionType.CALL_SKILL) {
            throw new IllegalArgumentException(
                "SkillDecision type must be "
                    + "CALL_SKILL"
            );
        }

        if (skillId == null
            || skillId.isBlank()
        ) {
            throw new IllegalArgumentException(
                "skillId cannot be blank"
            );
        }

        arguments =
            arguments == null
                ? Map.of()
                : Map.copyOf(arguments);
    }
}
