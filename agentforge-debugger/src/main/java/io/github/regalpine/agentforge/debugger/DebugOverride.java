package io.github.regalpine.agentforge.debugger;

import java.util.Map;
import java.util.Objects;

/**
 * 调试覆盖。
 *
 * <p>所有 Override 必须可追踪。
 * 对应 AF-ENHANCE-04 §29。</p>
 */
public record DebugOverride(
    String target,
    Object originalValue,
    Object overrideValue,
    String reason,
    String actorId
) {

    public DebugOverride {
        Objects.requireNonNull(
            target, "target"
        );
        Objects.requireNonNull(
            overrideValue, "overrideValue"
        );
    }

    public static DebugOverride of(
        String target,
        Object value,
        String reason
    ) {
        return new DebugOverride(
            target, null, value,
            reason, "debugger"
        );
    }
}
