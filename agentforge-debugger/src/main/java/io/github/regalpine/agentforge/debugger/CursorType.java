package io.github.regalpine.agentforge.debugger;

/**
 * 游标类型。
 *
 * <p>对应 AF-ENHANCE-04 §10。</p>
 */
public enum CursorType {

    EXECUTION,

    PLAN_STEP,

    SKILL,

    MODEL,

    TOOL,

    MEMORY,

    POLICY,

    APPROVAL,

    EVENT
}
