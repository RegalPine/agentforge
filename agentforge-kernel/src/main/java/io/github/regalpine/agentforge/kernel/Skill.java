package io.github.regalpine.agentforge.kernel;

import java.util.Optional;

/**
 * Skill 语义接口 — 不可变任务能力定义.
 * ASM-SKILL-01 §4: Skill 本体实体.
 * AF-AD-15 §4.1: 核心实体.
 *
 * <p>Skill 是 AgentForge 四级能力结构中的
 * "可复用任务能力"层：
 * <pre>
 * Agent → Skill → Capability → Execution
 * </pre>
 *
 * <p>深度不可变。所有返回的集合和引用均为
 * 防御拷贝，修改 Builder 不影响已构建对象。
 */
public interface Skill extends Entity {

    /**
     * Skill 名称（人类可读）。
     */
    String name();

    /**
     * Skill 目标描述（语义定义）。
     */
    String goal();

    /**
     * 输入输出契约。
     */
    SkillContract contract();

    /**
     * 依赖声明。
     */
    SkillDependencies dependencies();

    /**
     * 执行策略。
     */
    SkillExecutionStrategy executionStrategy();

    /**
     * 类型化的 Skill ID.
     */
    SkillId skillId();

    /**
     * 可选的策略引用。
     * S3 阶段扩展。
     */
    default Optional<Reference> policyReference() {
        return Optional.empty();
    }

    /**
     * 可选的评估契约引用。
     * S4 阶段扩展。
     */
    default Optional<Reference> evaluationContractRef() {
        return Optional.empty();
    }
}
