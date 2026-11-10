package io.github.regalpine.agentforge.debugger;

/**
 * 断点类型（10 种）。
 *
 * <p>对应 AF-ENHANCE-04 §16。</p>
 */
public enum BreakpointType {

    EXECUTION,

    PLAN_STEP,

    SKILL,

    TOOL,

    MODEL,

    MEMORY,

    POLICY,

    APPROVAL,

    EVENT,

    ERROR
}
