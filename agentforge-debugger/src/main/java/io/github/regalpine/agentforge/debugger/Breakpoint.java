package io.github.regalpine.agentforge.debugger;

import java.util.Objects;
import java.util.UUID;

/**
 * 断点。
 *
 * <p>Debugger 核心控制对象。
 * 对应 AF-ENHANCE-04 §15。</p>
 */
public record Breakpoint(
    String id,
    BreakpointType type,
    BreakpointCondition condition,
    boolean enabled,
    String targetId
) {

    public Breakpoint {
        Objects.requireNonNull(
            id, "id"
        );
        Objects.requireNonNull(
            type, "type"
        );
        if (condition == null) {
            condition =
                BreakpointCondition
                    .always();
        }
    }

    public static Breakpoint of(
        BreakpointType type
    ) {
        return new Breakpoint(
            "bp-" + UUID.randomUUID(),
            type,
            BreakpointCondition.always(),
            true,
            null
        );
    }

    public static Breakpoint of(
        BreakpointType type,
        String targetId
    ) {
        return new Breakpoint(
            "bp-" + UUID.randomUUID(),
            type,
            BreakpointCondition.always(),
            true,
            targetId
        );
    }

    public static Breakpoint conditional(
        BreakpointType type,
        BreakpointCondition condition
    ) {
        return new Breakpoint(
            "bp-" + UUID.randomUUID(),
            type,
            condition,
            true,
            null
        );
    }

    /**
     * 判断是否触发。
     */
    public boolean shouldTrigger(
        DebugContext context
    ) {
        if (!enabled) {
            return false;
        }
        return condition.matches(context);
    }

    /**
     * 创建启用/禁用副本。
     */
    public Breakpoint withEnabled(
        boolean enabled
    ) {
        return new Breakpoint(
            id, type, condition,
            enabled, targetId
        );
    }
}
