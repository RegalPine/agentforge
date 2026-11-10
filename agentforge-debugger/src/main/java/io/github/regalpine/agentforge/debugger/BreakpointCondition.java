package io.github.regalpine.agentforge.debugger;

import java.util.Map;
import java.util.Objects;

/**
 * 断点条件。
 *
 * <p>无副作用、有超时。
 * 对应 AF-ENHANCE-04 §19。</p>
 */
public interface BreakpointCondition {

    /**
     * 判断是否匹配。
     */
    boolean matches(
        DebugContext context
    );

    /**
     * 简单属性匹配条件。
     */
    static BreakpointCondition
    attributeEquals(
        String key,
        Object value
    ) {
        Objects.requireNonNull(
            key, "key"
        );
        return ctx -> {
            if (ctx == null) {
                return false;
            }
            Map<String, Object> attrs =
                ctx.attributes();
            return attrs != null
                && value.equals(
                    attrs.get(key)
                );
        };
    }

    /**
     * 始终匹配。
     */
    static BreakpointCondition always() {
        return ctx -> true;
    }

    /**
     * 永不匹配。
     */
    static BreakpointCondition never() {
        return ctx -> false;
    }
}
