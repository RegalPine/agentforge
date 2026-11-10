package io.github.regalpine.agentforge.sdk;

import java.time.Instant;
import java.util.Map;
import java.util.Objects;

/**
 * SDK-level Skill execution result.
 * AF-SKILL-04 §24: SkillExecutionResult.
 *
 * <p>Developer-facing result wrapper.
 * Contains the Skill output, status,
 * and trace information.
 *
 * <p>Immutable. Output map is defensively
 * copied.
 */
public record SkillExecutionResult(
    String skillId,
    String version,
    Status status,
    Map<String, Object> output,
    String errorCode,
    String errorMessage,
    Instant completedAt
) {

    public enum Status {
        SUCCESS,
        FAILED,
        TIMEOUT,
        CANCELLED,
        SKILL_NOT_FOUND,
        PERMISSION_DENIED
    }

    public SkillExecutionResult {
        Objects.requireNonNull(status, "status");
        output = output == null
            ? Map.of()
            : Map.copyOf(output);
        completedAt = completedAt == null
            ? Instant.now()
            : completedAt;
    }

    /**
     * Create a success result.
     */
    public static SkillExecutionResult success(
        String skillId,
        String version,
        Map<String, Object> output
    ) {
        return new SkillExecutionResult(
            skillId, version, Status.SUCCESS,
            output, null, null, Instant.now()
        );
    }

    /**
     * Create a failure result.
     */
    public static SkillExecutionResult failure(
        String skillId,
        String version,
        String errorCode,
        String errorMessage
    ) {
        return new SkillExecutionResult(
            skillId, version, Status.FAILED,
            Map.of(), errorCode, errorMessage,
            Instant.now()
        );
    }

    /**
     * Create from kernel SkillResult.
     */
    public static SkillExecutionResult from(
        io.github.regalpine.agentforge.skill.governance.SkillResult kernel
    ) {
        Objects.requireNonNull(kernel, "kernel");

        Status sdkStatus = kernel.isSuccess()
            ? Status.SUCCESS
            : mapStatus(kernel.status());

        return new SkillExecutionResult(
            kernel.skill().skillId().value(),
            kernel.skill().version().value(),
            sdkStatus,
            kernel.output(),
            kernel.errorCode() != null
                ? kernel.errorCode().name()
                : null,
            kernel.errorMessage(),
            kernel.completedAt()
        );
    }

    private static Status mapStatus(
        io.github.regalpine.agentforge.skill.governance.SkillResultStatus s
    ) {
        return switch (s) {
            case COMPLETED -> Status.SUCCESS;
            case FAILED -> Status.FAILED;
            case TIMEOUT -> Status.TIMEOUT;
            case CANCELLED -> Status.CANCELLED;
            case WAITING_HUMAN -> Status.FAILED;
        };
    }
}
