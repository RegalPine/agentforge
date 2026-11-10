package io.github.regalpine.agentforge.capability;

import io.github.regalpine.agentforge.kernel.EntityId;
import io.github.regalpine.agentforge.kernel.Reference;
import io.github.regalpine.agentforge.kernel.Version;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for AE-02 Capability Enhancement:
 * types, side effects, lifecycle, health,
 * retry policy, invocation pipeline.
 */
class CapabilityEnhancementTest {

    // --- CapabilityType ---

    @Test
    void shouldHaveAllCapabilityTypes() {
        assertEquals(6, CapabilityType.values().length);

        assertNotNull(
            CapabilityType.valueOf("MODEL")
        );

        assertNotNull(
            CapabilityType.valueOf("TOOL")
        );

        assertNotNull(
            CapabilityType.valueOf("AGENT")
        );

        assertNotNull(
            CapabilityType.valueOf("RETRIEVAL")
        );

        assertNotNull(
            CapabilityType.valueOf("MEMORY")
        );

        assertNotNull(
            CapabilityType.valueOf("HUMAN")
        );
    }

    // --- SideEffectClassification ---

    @Test
    void shouldHaveAllSideEffects() {
        assertEquals(
            3,
            SideEffectClassification.values().length
        );

        assertNotNull(
            SideEffectClassification.READ_ONLY
        );

        assertNotNull(
            SideEffectClassification.WRITE
        );

        assertNotNull(
            SideEffectClassification.EXTERNAL_SIDE_EFFECT
        );
    }

    // --- CapabilityStatus ---

    @Test
    void shouldHaveAllLifecycleStatuses() {
        assertEquals(
            5,
            CapabilityStatus.values().length
        );
    }

    // --- CapabilityHealth ---

    @Test
    void shouldHaveAllHealthStatuses() {
        assertEquals(
            4,
            CapabilityHealth.values().length
        );
    }

    // --- CapabilityErrorType ---

    @Test
    void shouldHaveAllErrorTypes() {
        assertTrue(
            CapabilityErrorType.values().length >= 10
        );
    }

    // --- RetryPolicy ---

    @Test
    void shouldCreateNoRetryPolicy() {
        var policy = RetryPolicy.none();

        assertEquals(0, policy.maxRetries());
    }

    @Test
    void shouldCreateFixedRetryPolicy() {
        var policy =
            RetryPolicy.fixed(3);

        assertEquals(3, policy.maxRetries());
    }

    @Test
    void shouldComputeExponentialDelay() {
        var policy = new RetryPolicy(
            3,
            Duration.ofMillis(100),
            2.0,
            Duration.ofSeconds(10)
        );

        assertEquals(
            100,
            policy.delayForAttempt(0).toMillis()
        );

        assertEquals(
            200,
            policy.delayForAttempt(1).toMillis()
        );

        assertEquals(
            400,
            policy.delayForAttempt(2).toMillis()
        );
    }

    @Test
    void shouldCapDelayAtMaxDelay() {
        var policy = new RetryPolicy(
            10,
            Duration.ofMillis(100),
            10.0,
            Duration.ofMillis(500)
        );

        assertTrue(
            policy.delayForAttempt(5).toMillis()
                <= 500
        );
    }

    // --- CapabilityContract (enhanced) ---

    @Test
    void shouldCreateEnhancedContract() {
        var contract = new CapabilityContract(
            Duration.ofSeconds(10),
            true,
            java.util.Set.of("read"),
            CapabilityType.TOOL,
            SideEffectClassification.READ_ONLY,
            RetryPolicy.fixed(3)
        );

        assertEquals(
            CapabilityType.TOOL,
            contract.type()
        );

        assertEquals(
            SideEffectClassification.READ_ONLY,
            contract.sideEffect()
        );

        assertEquals(
            3,
            contract.retryPolicy().maxRetries()
        );
    }

    @Test
    void shouldDefaultContractValues() {
        var contract =
            CapabilityContract.defaults();

        assertEquals(
            CapabilityType.TOOL,
            contract.type()
        );

        assertEquals(
            SideEffectClassification.READ_ONLY,
            contract.sideEffect()
        );

        assertEquals(
            0,
            contract.retryPolicy().maxRetries()
        );
    }

    // --- InvocationContext (enhanced) ---

    @Test
    void shouldCreateContextWithIdempotencyKey() {
        var ctx = new InvocationContext(
            "exec-1",
            "tenant-1",
            "idem-key-123",
            Map.of()
        );

        assertEquals(
            "idem-key-123",
            ctx.idempotencyKey()
        );
    }

    @Test
    void shouldCreateContextWithoutIdempotencyKey() {
        var ctx = new InvocationContext(
            "exec-1",
            "tenant-1",
            Map.of()
        );

        assertNull(ctx.idempotencyKey());
    }

    // --- InvocationPipeline ---

    @Test
    void shouldExecutePipelineSuccessfully() {
        var registry =
            new InMemoryCapabilityRegistry();

        var definition = new CapabilityDefinition(
            new EntityId("test.tool"),
            new Version("1.0.0"),
            "test.tool",
            null,
            null,
            CapabilityContract.defaults()
        );

        registry.register(definition);

        var provider = new StubProvider(
            Map.of("result", "ok")
        );

        var pipeline =
            InvocationPipeline.builder()
                .registry(registry)
                .provider("test", provider)
                .build();

        var invocation = new CapabilityInvocation(
            "inv-1",
            new EntityId("test.tool"),
            new Version("1.0.0"),
            Map.of("key", "value"),
            new InvocationContext(
                "exec-1", "t1", Map.of()
            )
        );

        PipelineResult result =
            pipeline.execute(invocation);

        assertTrue(result.isSuccess());

        assertEquals(
            InvocationStage.RETURN,
            result.completedStage()
        );

        assertEquals(1, result.attempts());
    }

    @Test
    void shouldFailOnUnknownCapability() {
        var registry =
            new InMemoryCapabilityRegistry();

        var pipeline =
            InvocationPipeline.builder()
                .registry(registry)
                .build();

        var invocation = new CapabilityInvocation(
            "inv-2",
            new EntityId("unknown.tool"),
            new Version("1.0.0"),
            Map.of(),
            new InvocationContext(
                "exec-2", "t1", Map.of()
            )
        );

        assertThrows(
            InvocationPipeline.CapabilityNotFoundException.class,
            () -> pipeline.execute(invocation)
        );
    }

    @Test
    void shouldRetryOnFailure() {
        var registry =
            new InMemoryCapabilityRegistry();

        var contract = new CapabilityContract(
            Duration.ofSeconds(10),
            false,
            java.util.Set.of(),
            CapabilityType.TOOL,
            SideEffectClassification.READ_ONLY,
            RetryPolicy.fixed(2)
        );

        var definition = new CapabilityDefinition(
            new EntityId("retry.tool"),
            new Version("1.0.0"),
            "retry.tool",
            null,
            null,
            contract
        );

        registry.register(definition);

        var callCount = new AtomicInteger(0);

        var provider = new CapabilityProvider() {
            @Override
            public ProviderDescriptor descriptor() {
                return new ProviderDescriptor(
                    "retry", new Version("1.0.0"),
                    java.util.Set.of(), Map.of()
                );
            }

            @Override
            public CapabilityResult invoke(
                CapabilityInvocation inv
            ) {
                int count =
                    callCount.incrementAndGet();

                if (count < 3) {
                    return new CapabilityFailure(
                        "TRANSIENT",
                        "transient error",
                        true
                    );
                }

                return new CapabilitySuccess(
                    Map.of("result", "ok")
                );
            }
        };

        var pipeline =
            InvocationPipeline.builder()
                .registry(registry)
                .provider("retry", provider)
                .build();

        var invocation = new CapabilityInvocation(
            "inv-3",
            new EntityId("retry.tool"),
            new Version("1.0.0"),
            Map.of(),
            new InvocationContext(
                "exec-3", "t1", Map.of()
            )
        );

        PipelineResult result =
            pipeline.execute(invocation);

        assertTrue(result.isSuccess());

        assertEquals(3, result.attempts());
    }

    @Test
    void shouldNotRetryNonRetryableFailure() {
        var registry =
            new InMemoryCapabilityRegistry();

        var definition = new CapabilityDefinition(
            new EntityId("noretry.tool"),
            new Version("1.0.0"),
            "noretry.tool",
            null,
            null,
            new CapabilityContract(
                Duration.ofSeconds(10),
                false,
                java.util.Set.of(),
                CapabilityType.TOOL,
                SideEffectClassification.WRITE,
                RetryPolicy.fixed(3)
            )
        );

        registry.register(definition);

        var callCount = new AtomicInteger(0);

        var provider = new CapabilityProvider() {
            @Override
            public ProviderDescriptor descriptor() {
                return new ProviderDescriptor(
                    "noretry", new Version("1.0.0"),
                    java.util.Set.of(), Map.of()
                );
            }

            @Override
            public CapabilityResult invoke(
                CapabilityInvocation inv
            ) {
                callCount.incrementAndGet();

                return new CapabilityFailure(
                    "BUSINESS_ERROR",
                    "non-retryable",
                    false
                );
            }
        };

        var pipeline =
            InvocationPipeline.builder()
                .registry(registry)
                .provider("noretry", provider)
                .build();

        var invocation = new CapabilityInvocation(
            "inv-4",
            new EntityId("noretry.tool"),
            new Version("1.0.0"),
            Map.of(),
            new InvocationContext(
                "exec-4", "t1", Map.of()
            )
        );

        PipelineResult result =
            pipeline.execute(invocation);

        assertFalse(result.isSuccess());

        assertEquals(1, callCount.get());
    }

    @Test
    void shouldShortCircuitViaInterceptor() {
        var registry =
            new InMemoryCapabilityRegistry();

        var definition = new CapabilityDefinition(
            new EntityId("auth.tool"),
            new Version("1.0.0"),
            "auth.tool",
            null,
            null,
            CapabilityContract.defaults()
        );

        registry.register(definition);

        var provider = new StubProvider(
            Map.of("result", "ok")
        );

        // Interceptor that denies at
        // AUTHORIZE stage
        var pipeline =
            InvocationPipeline.builder()
                .registry(registry)
                .provider("auth", provider)
                .interceptor((stage, inv, def) -> {
                    if (stage == InvocationStage.AUTHORIZE) {
                        return Optional.of(
                            new CapabilityFailure(
                                "POLICY_DENIED",
                                "Not authorized",
                                false
                            )
                        );
                    }

                    return Optional.empty();
                })
                .build();

        var invocation = new CapabilityInvocation(
            "inv-5",
            new EntityId("auth.tool"),
            new Version("1.0.0"),
            Map.of(),
            new InvocationContext(
                "exec-5", "t1", Map.of()
            )
        );

        PipelineResult result =
            pipeline.execute(invocation);

        assertFalse(result.isSuccess());

        assertInstanceOf(
            CapabilityFailure.class,
            result.result()
        );
    }

    // --- Stub provider ---

    private static class StubProvider
        implements CapabilityProvider {

        private final Map<String, Object> output;

        StubProvider(Map<String, Object> output) {
            this.output = output;
        }

        @Override
        public ProviderDescriptor descriptor() {
            return new ProviderDescriptor(
                "stub", new Version("1.0.0"),
                java.util.Set.of(), Map.of()
            );
        }

        @Override
        public CapabilityResult invoke(
            CapabilityInvocation invocation
        ) {
            return new CapabilitySuccess(output);
        }
    }
}
