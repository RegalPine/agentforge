package io.github.regalpine.agentforge.memory;

import java.util.Map;
import java.util.Objects;

/**
 * Memory access context.
 * AE-08 §17-18, §21: provides the
 * execution/actor information needed
 * for policy evaluation.
 */
public record MemoryAccessContext(
    String executionId,
    String tenantId,
    String agentId,
    String userId,
    String conversationId,
    Map<String, Object> attributes
) {

    public MemoryAccessContext {
        Objects.requireNonNull(
            tenantId, "tenantId"
        );

        executionId =
            executionId == null
                ? "" : executionId;
        agentId =
            agentId == null
                ? "" : agentId;
        userId =
            userId == null
                ? "" : userId;
        conversationId =
            conversationId == null
                ? "" : conversationId;
        attributes =
            attributes == null
                ? Map.of()
                : Map.copyOf(attributes);
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {

        private String executionId;
        private String tenantId;
        private String agentId;
        private String userId;
        private String conversationId;
        private Map<String, Object> attributes;

        private Builder() {}

        public Builder executionId(String v) {
            this.executionId = v; return this;
        }

        public Builder tenantId(String v) {
            this.tenantId = v; return this;
        }

        public Builder agentId(String v) {
            this.agentId = v; return this;
        }

        public Builder userId(String v) {
            this.userId = v; return this;
        }

        public Builder conversationId(String v) {
            this.conversationId = v; return this;
        }

        public Builder attributes(Map<String, Object> v) {
            this.attributes = v; return this;
        }

        public MemoryAccessContext build() {
            return new MemoryAccessContext(
                executionId, tenantId, agentId,
                userId, conversationId, attributes
            );
        }
    }
}
