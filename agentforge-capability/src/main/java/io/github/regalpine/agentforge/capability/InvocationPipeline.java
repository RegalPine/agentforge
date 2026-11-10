package io.github.regalpine.agentforge.capability;

import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;

/**
 * Invocation Pipeline.
 * AE-02 §24: Resolve → Validate → Authorize →
 * Budget → Observe → Execute → Validate Result →
 * Audit → Return.
 *
 * <p>The pipeline is composable: each stage can
 * be customised via interceptors. The default
 * implementation performs all stages in order
 * and supports retry according to the
 * capability's RetryPolicy.
 */
public final class InvocationPipeline {

    /**
     * Functional interface for pipeline
     * interceptors. An interceptor can
     * short-circuit the pipeline by returning
     * a CapabilityResult directly.
     */
    @FunctionalInterface
    public interface PipelineInterceptor {

        /**
         * Called before the given stage.
         * Return an Optional containing a
         * result to short-circuit, or
         * Optional.empty() to continue.
         */
        Optional<CapabilityResult> beforeStage(
            InvocationStage stage,
            CapabilityInvocation invocation,
            CapabilityDefinition definition
        );
    }

    private final CapabilityRegistry registry;
    private final Map<
        String,
        CapabilityProvider
    > providers;

    private final java.util.List<PipelineInterceptor> interceptors;

    private InvocationPipeline(
        CapabilityRegistry registry,
        Map<String, CapabilityProvider> providers,
        java.util.List<PipelineInterceptor> interceptors
    ) {
        this.registry = registry;
        this.providers = Map.copyOf(providers);
        this.interceptors =
            java.util.List.copyOf(interceptors);
    }

    /**
     * Execute a capability invocation
     * through the full pipeline.
     */
    public PipelineResult execute(
        CapabilityInvocation invocation
    ) {
        Instant start = Instant.now();
        Map<String, Object> meta = new HashMap<>();

        // 1. RESOLVE
        Optional<CapabilityResult> shortcut =
            runInterceptors(
                InvocationStage.RESOLVE,
                invocation,
                null
            );

        if (shortcut.isPresent()) {
            return buildResult(
                invocation, shortcut.get(),
                InvocationStage.RESOLVE,
                start, 1, meta
            );
        }

        CapabilityDefinition definition =
            resolveCapability(invocation);

        // 2. VALIDATE
        shortcut = runInterceptors(
            InvocationStage.VALIDATE,
            invocation, definition
        );

        if (shortcut.isPresent()) {
            return buildResult(
                invocation, shortcut.get(),
                InvocationStage.VALIDATE,
                start, 1, meta
            );
        }

        validateInput(invocation, definition);

        // 3. AUTHORIZE
        shortcut = runInterceptors(
            InvocationStage.AUTHORIZE,
            invocation, definition
        );

        if (shortcut.isPresent()) {
            return buildResult(
                invocation, shortcut.get(),
                InvocationStage.AUTHORIZE,
                start, 1, meta
            );
        }

        // 4. BUDGET
        shortcut = runInterceptors(
            InvocationStage.BUDGET,
            invocation, definition
        );

        if (shortcut.isPresent()) {
            return buildResult(
                invocation, shortcut.get(),
                InvocationStage.BUDGET,
                start, 1, meta
            );
        }

        // 5. OBSERVE
        runInterceptors(
            InvocationStage.OBSERVE,
            invocation, definition
        );

        // 6. EXECUTE (with retry)
        CapabilityProvider provider =
            resolveProvider(invocation);

        CapabilityResult result = null;
        int attempts = 0;
        RetryPolicy retryPolicy =
            definition.contract().retryPolicy();

        int maxAttempts =
            1 + retryPolicy.maxRetries();

        for (int attempt = 0;
             attempt < maxAttempts;
             attempt++
        ) {
            attempts = attempt + 1;

            try {
                result = provider.invoke(invocation);
            } catch (Exception e) {
                result = new CapabilityFailure(
                    CapabilityErrorType.REMOTE_ERROR.name(),
                    e.getMessage(),
                    true
                );
            }

            if (result instanceof CapabilitySuccess) {
                break;
            }

            if (result instanceof CapabilityFailure f
                && !f.retryable()
            ) {
                break;
            }

            if (attempt < maxAttempts - 1) {
                Duration delay =
                    retryPolicy.delayForAttempt(attempt);

                try {
                    Thread.sleep(delay.toMillis());
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();

                    result = new CapabilityFailure(
                        CapabilityErrorType.TIMEOUT.name(),
                        "Interrupted during retry",
                        false
                    );

                    break;
                }
            }
        }

        if (result == null) {
            result = new CapabilityFailure(
                CapabilityErrorType.UNKNOWN.name(),
                "No result produced",
                false
            );
        }

        // 7. VALIDATE RESULT
        if (result instanceof CapabilitySuccess) {
            // output validation could be
            // plugged in via interceptors
        }

        // 8. AUDIT
        runInterceptors(
            InvocationStage.AUDIT,
            invocation, definition
        );

        meta.put("capabilityType",
            definition.contract().type().name());

        meta.put("sideEffect",
            definition.contract().sideEffect().name());

        // 9. RETURN
        return buildResult(
            invocation, result,
            InvocationStage.RETURN,
            start, attempts, meta
        );
    }

    private Optional<CapabilityResult> runInterceptors(
        InvocationStage stage,
        CapabilityInvocation invocation,
        CapabilityDefinition definition
    ) {
        for (PipelineInterceptor interceptor :
            interceptors
        ) {
            Optional<CapabilityResult> r =
                interceptor.beforeStage(
                    stage, invocation, definition
                );

            if (r != null && r.isPresent()) {
                return r;
            }
        }

        return Optional.empty();
    }

    private CapabilityDefinition resolveCapability(
        CapabilityInvocation invocation
    ) {
        io.github.regalpine.agentforge.kernel.Reference ref =
            new io.github.regalpine.agentforge.kernel.Reference(
                invocation.capabilityId(),
                invocation.capabilityVersion()
            );

        return registry.find(ref).orElseThrow(
            () -> new CapabilityNotFoundException(
                invocation.capabilityId().value()
            )
        );
    }

    private CapabilityProvider resolveProvider(
        CapabilityInvocation invocation
    ) {
        String capId =
            invocation.capabilityId().value();

        for (var entry : providers.entrySet()) {
            if (capId.startsWith(entry.getKey())) {
                return entry.getValue();
            }
        }

        // fallback: first provider
        if (!providers.isEmpty()) {
            return providers.values().iterator().next();
        }

        throw new IllegalStateException(
            "No provider found for capability: "
                + capId
        );
    }

    private void validateInput(
        CapabilityInvocation invocation,
        CapabilityDefinition definition
    ) {
        // Schema validation: if inputSchema
        // is defined and arguments are empty
        // when schema requires fields, fail.
        // For now, basic null-check only.
        if (invocation.arguments() == null) {
            throw new CapabilityValidationException(
                "Arguments must not be null"
            );
        }
    }

    private PipelineResult buildResult(
        CapabilityInvocation invocation,
        CapabilityResult result,
        InvocationStage stage,
        Instant start,
        int attempts,
        Map<String, Object> meta
    ) {
        return new PipelineResult(
            invocation.invocationId(),
            result,
            stage,
            Duration.between(start, Instant.now()),
            attempts,
            meta
        );
    }

    /**
     * Builder for InvocationPipeline.
     */
    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {

        private CapabilityRegistry registry;
        private final Map<
            String,
            CapabilityProvider
        > providers = new HashMap<>();

        private final java.util.List<PipelineInterceptor> interceptors =
            new java.util.ArrayList<>();

        private Builder() {}

        public Builder registry(
            CapabilityRegistry registry
        ) {
            this.registry = registry;
            return this;
        }

        public Builder provider(
            String providerId,
            CapabilityProvider provider
        ) {
            this.providers.put(providerId, provider);
            return this;
        }

        public Builder interceptor(
            PipelineInterceptor interceptor
        ) {
            this.interceptors.add(interceptor);
            return this;
        }

        public InvocationPipeline build() {
            Objects.requireNonNull(
                registry, "registry"
            );

            return new InvocationPipeline(
                registry, providers, interceptors
            );
        }
    }

    /**
     * Thrown when a capability is not found
     * in the registry.
     */
    public static class CapabilityNotFoundException
        extends RuntimeException {

        public CapabilityNotFoundException(
            String capabilityId
        ) {
            super("Capability not found: " + capabilityId);
        }
    }

    /**
     * Thrown when input validation fails.
     */
    public static class CapabilityValidationException
        extends RuntimeException {

        public CapabilityValidationException(
            String message
        ) {
            super(message);
        }
    }
}
