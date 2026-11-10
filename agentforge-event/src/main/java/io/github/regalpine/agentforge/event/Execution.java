package io.github.regalpine.agentforge.event;

import io.github.regalpine.agentforge.event.ExecutionId;

import io.github.regalpine.agentforge.kernel.EntityId;
import io.github.regalpine.agentforge.kernel.SkillReference;
import io.github.regalpine.agentforge.kernel.Version;

import java.time.Instant;
import java.util.Optional;

/**
 * Execution 运行时事实.
 * AF-SKILL-01 §18: Execution 扩展.
 * AF-AD-15 §10.1: Execution 增加 Skill 字段.
 *
 * <p>不存在 SkillExecution 新类型。
 * Skill Execution 只是
 * {@code Execution.skillReference != null}。
 */
public record Execution(
    ExecutionId id,
    EntityId graphId,
    Version graphVersion,
    String planFingerprint,
    ExecutionStatus status,
    Instant createdAt,
    Instant startedAt,
    Instant completedAt,
    SkillReference skill
) {

    public Execution {
        // skill is optional — null means
        // this is not a Skill execution
    }

    /**
     * Convenience constructor without
     * Skill reference (backwards compatible).
     */
    public Execution(
        ExecutionId id,
        EntityId graphId,
        Version graphVersion,
        String planFingerprint,
        ExecutionStatus status,
        Instant createdAt,
        Instant startedAt,
        Instant completedAt
    ) {
        this(
            id, graphId, graphVersion,
            planFingerprint, status,
            createdAt, startedAt,
            completedAt, null
        );
    }

    /**
     * @return the Skill reference if this
     *         Execution is a Skill execution.
     */
    public Optional<SkillReference> skillOpt() {
        return Optional.ofNullable(skill);
    }
}
