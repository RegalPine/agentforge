package io.github.regalpine.agentforge.debugger;

import java.util.Map;
import java.util.Objects;

/**
 * 调试上下文。
 *
 * <p>提供断点条件评估所需的上下文。
 * 对应 AF-ENHANCE-04 §19。</p>
 */
public record DebugContext(
    String executionId,
    String eventType,
    String spanId,
    String agentId,
    String skillId,
    String toolId,
    String modelId,
    Map<String, Object> attributes
) {

    public DebugContext {
        Objects.requireNonNull(
            executionId, "executionId"
        );
        attributes = attributes == null
            ? Map.of()
            : Map.copyOf(attributes);
    }

    public static DebugContext of(
        String executionId,
        String eventType
    ) {
        return new DebugContext(
            executionId, eventType,
            null, null, null,
            null, null, Map.of()
        );
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {

        private String executionId;
        private String eventType;
        private String spanId;
        private String agentId;
        private String skillId;
        private String toolId;
        private String modelId;
        private Map<String, Object>
            attributes = Map.of();

        Builder() {}

        public Builder executionId(
            String v
        ) {
            this.executionId = v;
            return this;
        }

        public Builder eventType(
            String v
        ) {
            this.eventType = v;
            return this;
        }

        public Builder spanId(
            String v
        ) {
            this.spanId = v;
            return this;
        }

        public Builder agentId(
            String v
        ) {
            this.agentId = v;
            return this;
        }

        public Builder skillId(
            String v
        ) {
            this.skillId = v;
            return this;
        }

        public Builder toolId(
            String v
        ) {
            this.toolId = v;
            return this;
        }

        public Builder modelId(
            String v
        ) {
            this.modelId = v;
            return this;
        }

        public Builder attributes(
            Map<String, Object> v
        ) {
            this.attributes = v;
            return this;
        }

        public DebugContext build() {
            return new DebugContext(
                executionId, eventType,
                spanId, agentId,
                skillId, toolId,
                modelId, attributes
            );
        }
    }
}
