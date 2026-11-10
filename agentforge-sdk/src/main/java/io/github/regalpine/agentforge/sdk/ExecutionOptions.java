package io.github.regalpine.agentforge.sdk;

import java.time.Duration;
import java.util.Map;
import java.util.Objects;

/**
 * Options for controlling execution behavior.
 * <p>
 * Supports timeout, budget, streaming, idempotency key, and custom metadata.
 */
public final class ExecutionOptions {

    private final Duration timeout;
    private final boolean streaming;
    private final String idempotencyKey;
    private final Map<String, Object> metadata;

    private ExecutionOptions(Builder builder) {
        this.timeout = builder.timeout;
        this.streaming = builder.streaming;
        this.idempotencyKey = builder.idempotencyKey;
        this.metadata = builder.metadata == null ? Map.of() : Map.copyOf(builder.metadata);
    }

    public Duration timeout() {
        return timeout;
    }

    public boolean isStreaming() {
        return streaming;
    }

    public String idempotencyKey() {
        return idempotencyKey;
    }

    public Map<String, Object> metadata() {
        return metadata;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static ExecutionOptions defaults() {
        return builder().build();
    }

    public static final class Builder {
        private Duration timeout;
        private boolean streaming;
        private String idempotencyKey;
        private Map<String, Object> metadata;

        private Builder() {}

        public Builder timeout(Duration timeout) {
            this.timeout = timeout;
            return this;
        }

        public Builder streaming(boolean streaming) {
            this.streaming = streaming;
            return this;
        }

        public Builder idempotencyKey(String idempotencyKey) {
            this.idempotencyKey = idempotencyKey;
            return this;
        }

        public Builder metadata(Map<String, Object> metadata) {
            this.metadata = metadata;
            return this;
        }

        public ExecutionOptions build() {
            return new ExecutionOptions(this);
        }
    }
}
