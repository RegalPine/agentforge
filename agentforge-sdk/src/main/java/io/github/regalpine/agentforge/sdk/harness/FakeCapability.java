package io.github.regalpine.agentforge.sdk.harness;

import io.github.regalpine.agentforge.sdk.CapabilityDescriptor;
import io.github.regalpine.agentforge.sdk.CapabilityRef;
import io.github.regalpine.agentforge.sdk.InvocationResult;

import java.util.Map;
import java.util.Objects;
import java.util.function.Function;

/**
 * A fake Capability for testing purposes.
 * <p>
 * Can be configured with a handler function or a fixed response.
 * Tracks invocations for assertions.
 */
public final class FakeCapability implements CapabilityRef {

    private final String id;
    private final String version;
    private final Function<Map<String, Object>, InvocationResult> handler;
    private int invocationCount = 0;

    private FakeCapability(String id, String version, Function<Map<String, Object>, InvocationResult> handler) {
        this.id = Objects.requireNonNull(id);
        this.version = version != null ? version : "1.0.0";
        this.handler = Objects.requireNonNull(handler);
    }

    @Override
    public String id() {
        return id;
    }

    @Override
    public String version() {
        return version;
    }

    @Override
    public CapabilityDescriptor descriptor() {
        return new CapabilityDescriptor(id, version, "Fake capability for testing", null, null);
    }

    @Override
    public InvocationResult invoke(Map<String, Object> input) {
        invocationCount++;
        return handler.apply(input != null ? Map.copyOf(input) : Map.of());
    }

    public int invocationCount() {
        return invocationCount;
    }

    public static FakeCapability withResponse(String id, Map<String, Object> response) {
        return new FakeCapability(id, "1.0.0", input -> InvocationResult.success(response));
    }

    public static FakeCapability withHandler(String id, Function<Map<String, Object>, InvocationResult> handler) {
        return new FakeCapability(id, "1.0.0", handler);
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private String id;
        private String version = "1.0.0";
        private Function<Map<String, Object>, InvocationResult> handler;

        private Builder() {}

        public Builder id(String id) {
            this.id = id;
            return this;
        }

        public Builder version(String version) {
            this.version = version;
            return this;
        }

        public Builder handler(Function<Map<String, Object>, InvocationResult> handler) {
            this.handler = handler;
            return this;
        }

        public Builder response(Map<String, Object> response) {
            this.handler = input -> InvocationResult.success(response);
            return this;
        }

        public FakeCapability build() {
            return new FakeCapability(id, version, handler != null ? handler : input -> InvocationResult.success(Map.of()));
        }
    }
}
