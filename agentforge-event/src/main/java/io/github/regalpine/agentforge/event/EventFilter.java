package io.github.regalpine.agentforge.event;

import java.util.Objects;
import java.util.Set;

/**
 * 事件过滤器。
 *
 * <p>支持按 executionId / traceId / agentId /
 * skillId / toolId / eventTypes 组合过滤。</p>
 */
public record EventFilter(
    String executionId,
    String traceId,
    String agentId,
    String skillId,
    String toolId,
    Set<String> eventTypes
) {

    public EventFilter {
        if (eventTypes != null) {
            eventTypes = Set.copyOf(eventTypes);
        }
    }

    public static EventFilter all() {
        return new EventFilter(
            null, null, null,
            null, null, null
        );
    }

    public static EventFilter forExecution(
        String executionId
    ) {
        return new EventFilter(
            executionId, null, null,
            null, null, null
        );
    }

    public static EventFilter forTrace(
        String traceId
    ) {
        return new EventFilter(
            null, traceId, null,
            null, null, null
        );
    }

    public static EventFilter forAgent(
        String agentId
    ) {
        return new EventFilter(
            null, null, agentId,
            null, null, null
        );
    }

    public static EventFilter forTypes(
        String... types
    ) {
        return new EventFilter(
            null, null, null,
            null, null, Set.of(types)
        );
    }

    /**
     * 判断事件是否匹配此过滤器。
     */
    public boolean matches(EventEnvelope event) {
        Objects.requireNonNull(event);

        if (executionId != null
            && !executionId.equals(event.executionId())
        ) {
            return false;
        }
        if (traceId != null
            && !traceId.equals(event.traceId())
        ) {
            return false;
        }
        if (agentId != null
            && !agentId.equals(event.agentId())
        ) {
            return false;
        }
        if (skillId != null) {
            Object sid = event.payload().get("skillId");
            if (!skillId.equals(
                sid != null ? sid.toString() : null
            )) {
                return false;
            }
        }
        if (toolId != null) {
            Object tid = event.payload().get("toolId");
            if (!toolId.equals(
                tid != null ? tid.toString() : null
            )) {
                return false;
            }
        }
        if (eventTypes != null
            && !eventTypes.isEmpty()
            && !eventTypes.contains(event.eventType())
        ) {
            return false;
        }
        return true;
    }

    public Builder toBuilder() {
        return new Builder()
            .executionId(executionId)
            .traceId(traceId)
            .agentId(agentId)
            .skillId(skillId)
            .toolId(toolId)
            .eventTypes(eventTypes);
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private String executionId;
        private String traceId;
        private String agentId;
        private String skillId;
        private String toolId;
        private Set<String> eventTypes;

        Builder() {}

        public Builder executionId(String v) {
            this.executionId = v; return this;
        }
        public Builder traceId(String v) {
            this.traceId = v; return this;
        }
        public Builder agentId(String v) {
            this.agentId = v; return this;
        }
        public Builder skillId(String v) {
            this.skillId = v; return this;
        }
        public Builder toolId(String v) {
            this.toolId = v; return this;
        }
        public Builder eventTypes(Set<String> v) {
            this.eventTypes = v; return this;
        }

        public EventFilter build() {
            return new EventFilter(
                executionId, traceId, agentId,
                skillId, toolId, eventTypes
            );
        }
    }
}
