package io.github.regalpine.agentforge.sdk;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.function.Function;

/**
 * A simple AgentRef implementation backed by a function.
 * <p>
 * Useful for embedded mode and testing.
 */
public final class FunctionalAgentRef implements AgentRef {

    private final String id;
    private final String version;
    private final AgentDescriptor descriptor;
    private final Function<Map<String, Object>, ExecutionResult> handler;

    public FunctionalAgentRef(String id, String version, Function<Map<String, Object>, ExecutionResult> handler) {
        this.id = Objects.requireNonNull(id, "id cannot be null");
        this.version = version != null ? version : "1.0.0";
        this.handler = Objects.requireNonNull(handler, "handler cannot be null");
        this.descriptor = new AgentDescriptor(this.id, this.version, null, null);
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
    public AgentDescriptor descriptor() {
        return descriptor;
    }

    @Override
    public ExecutionResult run(Map<String, Object> input) {
        return handler.apply(input != null ? Map.copyOf(input) : Map.of());
    }

    @Override
    public ExecutionResult run(Map<String, Object> input, ExecutionOptions options) {
        return run(input);
    }

    @Override
    public CompletionStage<ExecutionResult> runAsync(Map<String, Object> input) {
        return CompletableFuture.supplyAsync(() -> run(input));
    }

    @Override
    public CompletionStage<ExecutionResult> runAsync(Map<String, Object> input, ExecutionOptions options) {
        return runAsync(input);
    }
}
