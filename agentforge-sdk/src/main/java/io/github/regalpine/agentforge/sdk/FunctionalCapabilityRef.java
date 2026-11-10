package io.github.regalpine.agentforge.sdk;

import java.util.Map;
import java.util.Objects;
import java.util.function.Function;

/**
 * A simple CapabilityRef implementation backed by a function.
 * <p>
 * Useful for embedded mode and testing.
 */
public final class FunctionalCapabilityRef implements CapabilityRef {

    private final String id;
    private final String version;
    private final CapabilityDescriptor descriptor;
    private final Function<Map<String, Object>, InvocationResult> handler;

    public FunctionalCapabilityRef(String id, String version, Function<Map<String, Object>, InvocationResult> handler) {
        this.id = Objects.requireNonNull(id, "id cannot be null");
        this.version = version != null ? version : "1.0.0";
        this.handler = Objects.requireNonNull(handler, "handler cannot be null");
        this.descriptor = new CapabilityDescriptor(this.id, this.version, null, null, null);
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
        return descriptor;
    }

    @Override
    public InvocationResult invoke(Map<String, Object> input) {
        return handler.apply(input != null ? Map.copyOf(input) : Map.of());
    }
}
