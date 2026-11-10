package io.github.regalpine.agentforge.model;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Model Provider & Routing tests.
 * AF-ENHANCE-09 §77: TestKit.
 *
 * <p>Covers: ModelLimits,
 * ModelParameters, ModelDefinition,
 * ModelHealthStatus, ModelHealth,
 * ModelLifecycle, CircuitBreaker,
 * ModelQuota, ModelBudget,
 * RoutingStrategy, RoutingContext,
 * ModelCandidate, RoutingReason,
 * RoutingDecision, DefaultModelRouter,
 * ModelCapabilityQuery,
 * DefaultModelResolver.
 */
class ModelProviderRoutingTest {

    // ====================
    // ModelLimits
    // ====================

    @Test
    void limits_defaults() {
        var limits = ModelLimits.defaults();
        assertEquals(128_000, limits.maxInputTokens());
        assertEquals(4_096, limits.maxOutputTokens());
        assertTrue(limits.maxContextTokens() > 0);
    }

    @Test
    void limits_small() {
        var limits = ModelLimits.small();
        assertEquals(8_000, limits.maxInputTokens());
        assertEquals(2_048, limits.maxOutputTokens());
    }

    @Test
    void limits_large() {
        var limits = ModelLimits.large();
        assertEquals(1_000_000, limits.maxInputTokens());
    }

    @Test
    void limits_fits() {
        var limits = ModelLimits.small();
        assertTrue(limits.fits(4_000));
        assertFalse(limits.fits(9_000));
    }

    @Test
    void limits_fitsInputAndOutput() {
        var limits = ModelLimits.small();
        assertTrue(limits.fits(4_000, 1_000));
        // 7000+2000=9000 <= 10248 context
        assertTrue(limits.fits(7_000, 2_000));
        // 8001 > maxInputTokens(8000)
        assertFalse(limits.fits(8_001, 100));
    }

    @Test
    void limits_negativeInputThrows() {
        assertThrows(
            IllegalArgumentException.class,
            () -> new ModelLimits(-1, 100, 200)
        );
    }

    @Test
    void limits_autoContextTokens() {
        var limits = new ModelLimits(1000, 200, -1);
        assertEquals(1200, limits.maxContextTokens());
    }

    // ====================
    // ModelParameters
    // ====================

    @Test
    void params_defaults() {
        var p = ModelParameters.defaults();
        assertEquals(0.7, p.temperature());
        assertEquals(1.0, p.topP());
        assertNull(p.seed());
    }

    @Test
    void params_deterministic() {
        var p = ModelParameters.deterministic();
        assertEquals(0.0, p.temperature());
        assertEquals(42L, p.seed());
    }

    @Test
    void params_builder() {
        var p = ModelParameters.builder()
            .temperature(0.5)
            .topP(0.9)
            .maxOutputTokens(1024)
            .seed(123L)
            .build();
        assertEquals(0.5, p.temperature());
        assertEquals(0.9, p.topP());
        assertEquals(1024, p.maxOutputTokens());
        assertEquals(123L, p.seed());
    }

    @Test
    void params_invalidTemperature() {
        assertThrows(
            IllegalArgumentException.class,
            () -> new ModelParameters(
                3.0, null, null, null, null
            )
        );
    }

    @Test
    void params_invalidTopP() {
        assertThrows(
            IllegalArgumentException.class,
            () -> new ModelParameters(
                null, 1.5, null, null, null
            )
        );
    }

    @Test
    void params_invalidMaxOutput() {
        assertThrows(
            IllegalArgumentException.class,
            () -> new ModelParameters(
                null, null, -1, null, null
            )
        );
    }

    // ====================
    // ModelDefinition
    // ====================

    @Test
    void definition_of() {
        var caps = ModelCapabilities.chatOnly();
        var def = ModelDefinition.of(
            "gpt-x", "openai", caps
        );
        assertEquals("gpt-x", def.modelId());
        assertEquals("openai", def.providerId());
        assertEquals("gpt-x", def.displayName());
        assertEquals("1.0.0", def.version());
    }

    @Test
    void definition_supports() {
        var caps = ModelCapabilities.builder()
            .chat(true)
            .toolCalling(true)
            .vision(true)
            .build();
        var def = ModelDefinition.of(
            "model-a", "prov", caps
        );
        assertTrue(def.supports("chat"));
        assertTrue(def.supports("toolCalling"));
        assertTrue(def.supports("vision"));
        assertFalse(def.supports("embedding"));
        assertFalse(def.supports("unknown"));
    }

    @Test
    void definition_withLimitsAndPricing() {
        var caps = ModelCapabilities.all();
        var limits = ModelLimits.large();
        var def = ModelDefinition.of(
            "model-b", "prov", caps, limits, null
        );
        assertEquals(1_000_000, def.limits().maxInputTokens());
    }

    @Test
    void definition_nullModelIdThrows() {
        assertThrows(
            NullPointerException.class,
            () -> ModelDefinition.of(
                null, "prov",
                ModelCapabilities.chatOnly()
            )
        );
    }

    // ====================
    // ModelHealthStatus
    // ====================

    @Test
    void healthStatus_isInvokable() {
        assertTrue(ModelHealthStatus.AVAILABLE.isInvokable());
        assertTrue(ModelHealthStatus.DEGRADED.isInvokable());
        assertFalse(ModelHealthStatus.UNAVAILABLE.isInvokable());
        assertFalse(ModelHealthStatus.DISABLED.isInvokable());
        assertFalse(ModelHealthStatus.REVOKED.isInvokable());
    }

    @Test
    void healthStatus_isTerminal() {
        assertTrue(ModelHealthStatus.REVOKED.isTerminal());
        assertFalse(ModelHealthStatus.AVAILABLE.isTerminal());
    }

    // ====================
    // ModelHealth
    // ====================

    @Test
    void health_healthy() {
        var h = ModelHealth.healthy("m1", "p1");
        assertTrue(h.isHealthy());
        assertEquals(ModelHealthStatus.AVAILABLE, h.status());
    }

    @Test
    void health_unhealthy() {
        var h = ModelHealth.unhealthy(
            "m1", "p1", "timeout"
        );
        assertFalse(h.isHealthy());
        assertEquals("timeout", h.reason());
    }

    // ====================
    // ModelLifecycle
    // ====================

    @Test
    void lifecycle_isInvokable() {
        assertTrue(ModelLifecycle.AVAILABLE.isInvokable());
        assertTrue(ModelLifecycle.IN_USE.isInvokable());
        assertFalse(ModelLifecycle.DEPRECATED.isInvokable());
        assertFalse(ModelLifecycle.REVOKED.isInvokable());
    }

    @Test
    void lifecycle_isTerminal() {
        assertTrue(ModelLifecycle.REVOKED.isTerminal());
        assertTrue(ModelLifecycle.INVALID.isTerminal());
        assertFalse(ModelLifecycle.AVAILABLE.isTerminal());
    }

    @Test
    void lifecycle_canTransition() {
        assertTrue(
            ModelLifecycle.REGISTERED.canTransitionTo(
                ModelLifecycle.VALIDATED
            )
        );
        assertTrue(
            ModelLifecycle.AVAILABLE.canTransitionTo(
                ModelLifecycle.IN_USE
            )
        );
        assertTrue(
            ModelLifecycle.DEPRECATED.canTransitionTo(
                ModelLifecycle.DISABLED
            )
        );
        assertFalse(
            ModelLifecycle.REVOKED.canTransitionTo(
                ModelLifecycle.AVAILABLE
            )
        );
        assertFalse(
            ModelLifecycle.INVALID.canTransitionTo(
                ModelLifecycle.AVAILABLE
            )
        );
    }

    @Test
    void lifecycle_isDeprecated() {
        assertTrue(ModelLifecycle.DEPRECATED.isDeprecated());
        assertFalse(ModelLifecycle.AVAILABLE.isDeprecated());
    }

    // ====================
    // CircuitBreakerState
    // ====================

    @Test
    void cbState_allowsRequests() {
        assertTrue(CircuitBreakerState.CLOSED.allowsRequests());
        assertTrue(CircuitBreakerState.HALF_OPEN.allowsRequests());
        assertFalse(CircuitBreakerState.OPEN.allowsRequests());
    }

    // ====================
    // CircuitBreaker
    // ====================

    @Test
    void cb_defaultClosed() {
        var cb = CircuitBreaker.of("test");
        assertEquals(CircuitBreakerState.CLOSED, cb.state());
        assertTrue(cb.allowRequest());
    }

    @Test
    void cb_openAfterFailures() {
        var cb = new CircuitBreaker(
            "test", 3, 1,
            Duration.ofSeconds(60)
        );
        cb.recordFailure();
        cb.recordFailure();
        assertEquals(CircuitBreakerState.CLOSED, cb.state());
        cb.recordFailure();
        assertEquals(CircuitBreakerState.OPEN, cb.state());
        assertFalse(cb.allowRequest());
    }

    @Test
    void cb_halfOpenAfterTimeout() {
        var cb = new CircuitBreaker(
            "test", 2, 1,
            Duration.ofMillis(1)
        );
        cb.recordFailure();
        cb.recordFailure();
        assertEquals(CircuitBreakerState.OPEN, cb.state());

        // Wait for timeout
        try { Thread.sleep(10); } catch (Exception e) {}

        assertEquals(CircuitBreakerState.HALF_OPEN, cb.state());
        assertTrue(cb.allowRequest());
    }

    @Test
    void cb_closeAfterHalfOpenSuccess() {
        var cb = new CircuitBreaker(
            "test", 2, 1,
            Duration.ofMillis(1)
        );
        cb.recordFailure();
        cb.recordFailure();
        try { Thread.sleep(10); } catch (Exception e) {}

        assertEquals(CircuitBreakerState.HALF_OPEN, cb.state());
        cb.recordSuccess();
        assertEquals(CircuitBreakerState.CLOSED, cb.state());
    }

    @Test
    void cb_reopenOnHalfOpenFailure() {
        var cb = new CircuitBreaker(
            "test", 2, 2,
            Duration.ofMillis(1)
        );
        cb.recordFailure();
        cb.recordFailure();
        try { Thread.sleep(10); } catch (Exception e) {}

        assertEquals(CircuitBreakerState.HALF_OPEN, cb.state());
        cb.recordFailure();
        assertEquals(CircuitBreakerState.OPEN, cb.state());
    }

    // ====================
    // ModelQuota
    // ====================

    @Test
    void quota_unlimited() {
        var q = ModelQuota.unlimited();
        assertTrue(q.tryAcquire(1000));
        q.release(500);
        assertEquals(500, q.getUsedTokens());
    }

    @Test
    void quota_limited() {
        var q = new ModelQuota("test", 3, 1000, 10);
        assertTrue(q.tryAcquire(100));
        assertTrue(q.tryAcquire(100));
        assertTrue(q.tryAcquire(100));
        assertFalse(q.tryAcquire(100));
        assertEquals(0, q.getRemainingRequests());
    }

    @Test
    void quota_concurrency() {
        var q = new ModelQuota("test", 100, 10000, 1);
        assertTrue(q.tryAcquire(100));
        assertFalse(q.tryAcquire(100));
        q.release(100);
        assertTrue(q.tryAcquire(100));
    }

    @Test
    void quota_reset() {
        var q = new ModelQuota("test", 2, 1000, 10);
        q.tryAcquire(100);
        q.tryAcquire(100);
        q.reset();
        assertEquals(0, q.getUsedRequests());
        assertTrue(q.tryAcquire(100));
    }

    // ====================
    // ModelBudget
    // ====================

    @Test
    void budget_unlimited() {
        var b = ModelBudget.unlimited();
        assertTrue(b.isWithinBudget());
        b.recordTokens(1000000);
        assertTrue(b.isWithinBudget());
    }

    @Test
    void budget_tokenLimit() {
        var b = new ModelBudget(
            1000, null, null, 100
        );
        assertTrue(b.isWithinBudget());
        b.recordTokens(500);
        assertTrue(b.isWithinBudget());
        b.recordTokens(600);
        assertFalse(b.isWithinBudget());
    }

    @Test
    void budget_costLimit() {
        var b = new ModelBudget(
            100000,
            new BigDecimal("1.00"),
            null, 100
        );
        assertTrue(b.isWithinBudget());
        b.recordCost(new BigDecimal("0.50"));
        assertTrue(b.isWithinBudget());
        b.recordCost(new BigDecimal("0.60"));
        assertFalse(b.isWithinBudget());
    }

    @Test
    void budget_invocationLimit() {
        var b = new ModelBudget(
            100000, null, null, 2
        );
        assertTrue(b.isWithinBudget());
        b.recordInvocation();
        assertTrue(b.isWithinBudget());
        b.recordInvocation();
        assertFalse(b.isWithinBudget());
    }

    @Test
    void budget_remainingTokens() {
        var b = new ModelBudget(
            1000, null, null, 100
        );
        b.recordTokens(300);
        assertEquals(700, b.getRemainingTokens());
    }

    // ====================
    // RoutingStrategy
    // ====================

    @Test
    void strategy_values() {
        assertEquals(10, RoutingStrategy.values().length);
        assertNotNull(RoutingStrategy.STATIC);
        assertNotNull(RoutingStrategy.PRIORITY);
        assertNotNull(RoutingStrategy.WEIGHTED);
        assertNotNull(RoutingStrategy.CAPABILITY);
        assertNotNull(RoutingStrategy.FAILOVER);
        assertNotNull(RoutingStrategy.COST_AWARE);
    }

    // ====================
    // RoutingContext
    // ====================

    @Test
    void context_empty() {
        var ctx = RoutingContext.empty();
        assertTrue(ctx.requiredCapabilities().isEmpty());
        assertNull(ctx.tenantId());
        assertNull(ctx.region());
    }

    @Test
    void context_builder() {
        var ctx = RoutingContext.builder()
            .capabilities(Set.of("chat", "vision"))
            .tenant("tenant-a")
            .region("eu")
            .build();
        assertEquals(2, ctx.requiredCapabilities().size());
        assertEquals("tenant-a", ctx.tenantId());
        assertEquals("eu", ctx.region());
    }

    // ====================
    // ModelCandidate
    // ====================

    @Test
    void candidate_of() {
        var def = ModelDefinition.of(
            "m1", "p1",
            ModelCapabilities.chatOnly()
        );
        var c = ModelCandidate.of(def);
        assertEquals("m1", c.modelId());
        assertEquals("p1", c.providerId());
        assertTrue(c.isAvailable());
    }

    @Test
    void candidate_withScore() {
        var def = ModelDefinition.of(
            "m1", "p1",
            ModelCapabilities.chatOnly()
        );
        var c = ModelCandidate.of(def);
        var scored = c.withScore(0.95);
        assertEquals(0.95, scored.score());
    }

    @Test
    void candidate_withStatus() {
        var def = ModelDefinition.of(
            "m1", "p1",
            ModelCapabilities.chatOnly()
        );
        var c = ModelCandidate.of(def);
        var rejected = c.withStatus("REJECTED");
        assertEquals("REJECTED", rejected.status());
    }

    @Test
    void candidate_unhealthy() {
        var def = ModelDefinition.of(
            "m1", "p1",
            ModelCapabilities.chatOnly()
        );
        var h = ModelHealth.unhealthy(
            "m1", "p1", "down"
        );
        var c = new ModelCandidate(
            def, h, 1.0, 50, 0.0, null, null
        );
        assertFalse(c.isAvailable());
    }

    // ====================
    // RoutingReason
    // ====================

    @Test
    void reason_selected() {
        var r = RoutingReason.selected(
            "m1", "p1", "best fit"
        );
        assertEquals(
            RoutingReason.RoutingVerdict.SELECTED,
            r.verdict()
        );
    }

    @Test
    void reason_rejected() {
        var r = RoutingReason.rejected(
            "m1", "p1", "quota exceeded"
        );
        assertEquals(
            RoutingReason.RoutingVerdict.REJECTED,
            r.verdict()
        );
    }

    // ====================
    // RoutingDecision
    // ====================

    @Test
    void decision_of() {
        var d = RoutingDecision.of(
            "m1", "p1",
            RoutingStrategy.STATIC
        );
        assertTrue(d.hasSelection());
        assertEquals("m1", d.selectedModelId());
    }

    @Test
    void decision_reasonFor() {
        var reasons = List.of(
            RoutingReason.selected("m1", "p1", "ok"),
            RoutingReason.rejected("m2", "p2", "bad")
        );
        var d = new RoutingDecision(
            "m1", "p1", List.of(), reasons,
            RoutingStrategy.STATIC, 5
        );
        assertNotNull(d.reasonFor("m1"));
        assertNotNull(d.reasonFor("m2"));
        assertNull(d.reasonFor("m3"));
    }

    // ====================
    // DefaultModelRouter
    // ====================

    @Test
    void router_staticSelectsFirst() {
        var router = DefaultModelRouter.of(
            RoutingStrategy.STATIC
        );
        var c1 = ModelCandidate.of(
            ModelDefinition.of(
                "m1", "p1",
                ModelCapabilities.chatOnly()
            )
        );
        var c2 = ModelCandidate.of(
            ModelDefinition.of(
                "m2", "p2",
                ModelCapabilities.chatOnly()
            )
        );
        var decision = router.route(
            List.of(c1, c2),
            RoutingContext.empty()
        );
        assertEquals("m1", decision.selectedModelId());
    }

    @Test
    void router_prioritySelectsLowest() {
        var router = DefaultModelRouter.of(
            RoutingStrategy.PRIORITY
        );
        var c1 = ModelCandidate.of(
            ModelDefinition.of("m1", "p1",
                ModelCapabilities.chatOnly()),
            10
        );
        var c2 = ModelCandidate.of(
            ModelDefinition.of("m2", "p2",
                ModelCapabilities.chatOnly()),
            1
        );
        var decision = router.route(
            List.of(c1, c2),
            RoutingContext.empty()
        );
        assertEquals("m2", decision.selectedModelId());
    }

    @Test
    void router_capabilityFilter() {
        var router = DefaultModelRouter.of(
            RoutingStrategy.STATIC
        );
        var chatOnly = ModelDefinition.of(
            "chat-model", "p1",
            ModelCapabilities.chatOnly()
        );
        var fullCaps = ModelDefinition.of(
            "full-model", "p2",
            ModelCapabilities.builder()
                .chat(true)
                .toolCalling(true)
                .vision(true)
                .build()
        );
        var ctx = RoutingContext.builder()
            .capabilities(Set.of("vision"))
            .build();
        var decision = router.route(
            List.of(
                ModelCandidate.of(chatOnly),
                ModelCandidate.of(fullCaps)
            ),
            ctx
        );
        assertEquals("full-model", decision.selectedModelId());
    }

    @Test
    void router_healthFilter() {
        var router = DefaultModelRouter.of(
            RoutingStrategy.STATIC
        );
        var healthy = ModelCandidate.of(
            ModelDefinition.of("m1", "p1",
                ModelCapabilities.chatOnly())
        );
        var unhealthy = new ModelCandidate(
            ModelDefinition.of("m2", "p2",
                ModelCapabilities.chatOnly()),
            ModelHealth.unhealthy("m2", "p2", "down"),
            1.0, 50, 0.0, null, null
        );
        var decision = router.route(
            List.of(unhealthy, healthy),
            RoutingContext.empty()
        );
        assertEquals("m1", decision.selectedModelId());
    }

    @Test
    void router_allUnhealthyThrows() {
        var router = DefaultModelRouter.of(
            RoutingStrategy.STATIC
        );
        var unhealthy = new ModelCandidate(
            ModelDefinition.of("m1", "p1",
                ModelCapabilities.chatOnly()),
            ModelHealth.unhealthy("m1", "p1", "down"),
            1.0, 50, 0.0, null, null
        );
        assertThrows(
            ModelUnavailableException.class,
            () -> router.route(
                List.of(unhealthy),
                RoutingContext.empty()
            )
        );
    }

    @Test
    void router_emptyCandidatesThrows() {
        var router = DefaultModelRouter.of(
            RoutingStrategy.STATIC
        );
        assertThrows(
            ModelUnavailableException.class,
            () -> router.route(
                List.of(),
                RoutingContext.empty()
            )
        );
    }

    @Test
    void router_hasReasons() {
        var router = DefaultModelRouter.of(
            RoutingStrategy.STATIC
        );
        var c1 = ModelCandidate.of(
            ModelDefinition.of("m1", "p1",
                ModelCapabilities.chatOnly())
        );
        var c2 = ModelCandidate.of(
            ModelDefinition.of("m2", "p2",
                ModelCapabilities.chatOnly())
        );
        var decision = router.route(
            List.of(c1, c2),
            RoutingContext.empty()
        );
        assertFalse(decision.reasons().isEmpty());
        assertTrue(decision.decisionTimeMs() >= 0);
    }

    @Test
    void router_costAwareSelectsCheapest() {
        var router = DefaultModelRouter.of(
            RoutingStrategy.COST_AWARE
        );
        var expensive = ModelDefinition.of(
            "expensive", "p1",
            ModelCapabilities.chatOnly()
        );
        // Manually set pricing
        var expensiveDef = new ModelDefinition(
            "expensive", "p1",
            null, null, null,
            ModelCapabilities.chatOnly(),
            null,
            ModelPricing.perMillionTokens(
                "expensive", "p1",
                new BigDecimal("10.00"),
                new BigDecimal("20.00"),
                java.util.Currency.getInstance("USD")
            ),
            null
        );
        var cheapDef = new ModelDefinition(
            "cheap", "p2",
            null, null, null,
            ModelCapabilities.chatOnly(),
            null,
            ModelPricing.perMillionTokens(
                "cheap", "p2",
                new BigDecimal("1.00"),
                new BigDecimal("2.00"),
                java.util.Currency.getInstance("USD")
            ),
            null
        );
        var decision = router.route(
            List.of(
                ModelCandidate.of(expensiveDef),
                ModelCandidate.of(cheapDef)
            ),
            RoutingContext.empty()
        );
        assertEquals("cheap", decision.selectedModelId());
    }

    // ====================
    // ModelCapabilityQuery
    // ====================

    @Test
    void query_of() {
        var q = ModelCapabilityQuery.of(
            Set.of("chat", "vision")
        );
        assertEquals(2, q.requiredCapabilities().size());
    }

    @Test
    void query_byProvider() {
        var q = ModelCapabilityQuery.byProvider("openai");
        assertEquals("openai", q.providerId());
    }

    @Test
    void query_matches() {
        var q = ModelCapabilityQuery.of(
            Set.of("chat", "toolCalling")
        );
        var def = ModelDefinition.of(
            "m1", "p1",
            ModelCapabilities.builder()
                .chat(true)
                .toolCalling(true)
                .build()
        );
        assertTrue(q.matches(def));

        var noTool = ModelDefinition.of(
            "m2", "p1",
            ModelCapabilities.chatOnly()
        );
        assertFalse(q.matches(noTool));
    }

    @Test
    void query_matchesProvider() {
        var q = ModelCapabilityQuery.byProvider("openai");
        var openai = ModelDefinition.of(
            "m1", "openai",
            ModelCapabilities.chatOnly()
        );
        var anthropic = ModelDefinition.of(
            "m2", "anthropic",
            ModelCapabilities.chatOnly()
        );
        assertTrue(q.matches(openai));
        assertFalse(q.matches(anthropic));
    }

    // ====================
    // DefaultModelResolver
    // ====================

    @Test
    void resolver_resolveByCapability() {
        var registry = new InMemoryModelRegistry();
        var resolver = DefaultModelResolver.of(registry);

        // Register a mock provider
        var provider = new MockModelProvider(
            "test-provider",
            List.of(
                ModelDescriptor.of(
                    new io.github.regalpine.agentforge.kernel.EntityId("model-a"),
                    new io.github.regalpine.agentforge.kernel.EntityId("test-provider"),
                    new io.github.regalpine.agentforge.kernel.Version("1.0.0"),
                    "Model A",
                    "A chat model",
                    ModelCapabilities.chatOnly()
                )
            )
        );
        registry.register(provider);

        var query = ModelCapabilityQuery.of(
            Set.of("chat")
        );
        var def = resolver.resolveByCapability(query);
        assertEquals("model-a", def.modelId());
    }

    @Test
    void resolver_noMatchThrows() {
        var registry = new InMemoryModelRegistry();
        var resolver = DefaultModelResolver.of(registry);

        var query = ModelCapabilityQuery.of(
            Set.of("vision")
        );
        assertThrows(
            ModelUnavailableException.class,
            () -> resolver.resolveByCapability(query)
        );
    }

    // ====================
    // Helper: Mock Provider
    // ====================

    static class MockModelProvider
        implements ModelProvider {

        private final String id;
        private final List<ModelDescriptor> models;

        MockModelProvider(
            String id,
            List<ModelDescriptor> models
        ) {
            this.id = id;
            this.models = models;
        }

        @Override
        public io.github.regalpine.agentforge.kernel.EntityId providerId() {
            return new io.github.regalpine.agentforge.kernel.EntityId(id);
        }

        @Override
        public io.github.regalpine.agentforge.kernel.Version version() {
            return new io.github.regalpine.agentforge.kernel.Version("1.0.0");
        }

        @Override
        public List<ModelDescriptor> availableModels() {
            return models;
        }

        @Override
        public ChatModel chatModel(
            io.github.regalpine.agentforge.kernel.EntityId modelId
        ) {
            throw new UnsupportedOperationException("mock");
        }

        @Override
        public EmbeddingModel embeddingModel(
            io.github.regalpine.agentforge.kernel.EntityId modelId
        ) {
            throw new UnsupportedOperationException("mock");
        }

        @Override
        public RerankModel rerankModel(
            io.github.regalpine.agentforge.kernel.EntityId modelId
        ) {
            throw new UnsupportedOperationException("mock");
        }

        @Override
        public StructuredOutputModel structuredOutputModel(
            io.github.regalpine.agentforge.kernel.EntityId modelId
        ) {
            throw new UnsupportedOperationException("mock");
        }

        @Override
        public ModelProviderHealth health() {
            return ModelProviderHealth.AVAILABLE;
        }

        @Override
        public boolean supports(
            io.github.regalpine.agentforge.kernel.EntityId modelId
        ) {
            return models.stream()
                .anyMatch(m -> m.modelId().equals(modelId));
        }
    }
}
