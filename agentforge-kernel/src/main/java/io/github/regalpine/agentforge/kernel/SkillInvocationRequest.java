package io.github.regalpine.agentforge.kernel;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Skill 调用请求.
 * AF-SKILL-01 §6: SkillInvocationRequest.
 * AF-AD-15 §6.2: 四种调用来源统一.
 *
 * <p>All four invocation sources
 * (Agent Loop / Graph Node / Application SDK /
 * Parent Skill) are converted to this type
 * before entering SkillInvoker.
 *
 * <p>Immutable. All maps are defensively
 * copied.
 */
public record SkillInvocationRequest(
    SkillReference skill,
    Map<String, Object> input,
    String callerId,
    String tenantId,
    long timeoutMillis
) {

    public SkillInvocationRequest {
        Objects.requireNonNull(skill, "skill");

        input =
            input == null
                ? Map.of()
                : Map.copyOf(input);

        if (timeoutMillis < 0) {
            throw new IllegalArgumentException(
                "timeoutMillis cannot be negative"
            );
        }
    }

    /**
     * Convenience constructor with defaults.
     */
    public SkillInvocationRequest(
        SkillReference skill,
        Map<String, Object> input
    ) {
        this(
            skill, input,
            null, null,
            30_000L
        );
    }

    /**
     * @return optional caller identifier.
     */
    public Optional<String> callerIdOpt() {
        return Optional.ofNullable(callerId);
    }

    /**
     * @return optional tenant identifier.
     */
    public Optional<String> tenantIdOpt() {
        return Optional.ofNullable(tenantId);
    }
}
