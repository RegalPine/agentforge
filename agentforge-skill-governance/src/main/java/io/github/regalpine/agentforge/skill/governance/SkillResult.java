package io.github.regalpine.agentforge.skill.governance;

import io.github.regalpine.agentforge.kernel.SkillReference;

import java.time.Instant;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Skill 调用结果.
 * AF-SKILL-01 §6: SkillResult.
 * AF-AD-15 §6.2: Skill 调用返回.
 *
 * <p>Wraps the execution output of a Skill
 * invocation. Contains the resolved Skill
 * reference, status, output data and optional
 * error information.
 *
 * <p>Immutable. All maps are defensively
 * copied.
 */
public record SkillResult(
    SkillReference skill,
    SkillResultStatus status,
    Map<String, Object> output,
    SkillErrorCode errorCode,
    String errorMessage,
    Instant completedAt
) {

    public SkillResult {
        Objects.requireNonNull(skill, "skill");
        Objects.requireNonNull(status, "status");
        Objects.requireNonNull(completedAt, "completedAt");

        output =
            output == null
                ? Map.of()
                : Map.copyOf(output);
    }

    /**
     * Create a successful result.
     */
    public static SkillResult success(
        SkillReference skill,
        Map<String, Object> output
    ) {
        return new SkillResult(
            skill,
            SkillResultStatus.COMPLETED,
            output,
            null,
            null,
            Instant.now()
        );
    }

    /**
     * Create a failed result.
     */
    public static SkillResult failed(
        SkillReference skill,
        SkillErrorCode code,
        String message
    ) {
        return new SkillResult(
            skill,
            SkillResultStatus.FAILED,
            Map.of(),
            code,
            message,
            Instant.now()
        );
    }

    /**
     * @return optional error code.
     */
    public Optional<SkillErrorCode> errorCodeOpt() {
        return Optional.ofNullable(errorCode);
    }

    /**
     * @return true if this result is
     *         successful.
     */
    public boolean isSuccess() {
        return status ==
            SkillResultStatus.COMPLETED;
    }
}
