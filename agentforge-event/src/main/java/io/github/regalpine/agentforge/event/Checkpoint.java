package io.github.regalpine.agentforge.event;

import java.time.Instant;
import java.util.Map;
import java.util.Objects;

/**
 * 执行检查点。
 *
 * <p>用于长时间运行 Execution 的状态快照。</p>
 */
public record Checkpoint(
    String checkpointId,
    String executionId,
    long sequence,
    Instant createdAt,
    Map<String, Object> state,
    Map<String, String> metadata
) {

    public Checkpoint {
        Objects.requireNonNull(
            checkpointId, "checkpointId"
        );
        Objects.requireNonNull(
            executionId, "executionId"
        );
        Objects.requireNonNull(
            createdAt, "createdAt"
        );
        state = state == null
            ? Map.of()
            : Map.copyOf(state);
        metadata = metadata == null
            ? Map.of()
            : Map.copyOf(metadata);
    }

    public static Checkpoint of(
        String executionId,
        long sequence,
        Map<String, Object> state
    ) {
        return new Checkpoint(
            "ckpt-" + EventEnvelope
                .generateEventId(),
            executionId,
            sequence,
            Instant.now(),
            state,
            Map.of()
        );
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private String checkpointId;
        private String executionId;
        private long sequence;
        private Instant createdAt;
        private Map<String, Object> state;
        private Map<String, String> metadata;

        Builder() {}

        public Builder checkpointId(String v) {
            this.checkpointId = v; return this;
        }
        public Builder executionId(String v) {
            this.executionId = v; return this;
        }
        public Builder sequence(long v) {
            this.sequence = v; return this;
        }
        public Builder createdAt(Instant v) {
            this.createdAt = v; return this;
        }
        public Builder state(Map<String, Object> v) {
            this.state = v; return this;
        }
        public Builder metadata(
            Map<String, String> v
        ) {
            this.metadata = v; return this;
        }

        public Checkpoint build() {
            if (checkpointId == null) {
                checkpointId = "ckpt-" +
                    EventEnvelope.generateEventId();
            }
            if (createdAt == null) {
                createdAt = Instant.now();
            }
            return new Checkpoint(
                checkpointId, executionId,
                sequence, createdAt,
                state, metadata
            );
        }
    }
}
