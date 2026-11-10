package io.github.regalpine.agentforge.kernel;

/**
 * Skill 执行策略枚举.
 * AF-SKILL-01 §6: Execution Strategy Resolution.
 * AF-AD-15 §6.3: 四种策略路由.
 *
 * <p>Defines how a Skill is executed at runtime:
 * <ul>
 *   <li>DIRECT_CAPABILITY — Skill → Capability Runtime</li>
 *   <li>GRAPH — Skill → GraphReference → GraphIR → ExecutionPlan → GraphRuntime</li>
 *   <li>AGENT — Skill → AgentReference → Agent Runtime</li>
 *   <li>COMPOSITE_SKILL — 多 Skill 顺序/组合执行</li>
 * </ul>
 */
public enum SkillExecutionStrategy {

    DIRECT_CAPABILITY,

    GRAPH,

    AGENT,

    COMPOSITE_SKILL
}
