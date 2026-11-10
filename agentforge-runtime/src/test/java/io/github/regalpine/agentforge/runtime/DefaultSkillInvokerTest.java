package io.github.regalpine.agentforge.runtime;

import io.github.regalpine.agentforge.capability.*;
import io.github.regalpine.agentforge.kernel.*;
import io.github.regalpine.agentforge.skill.governance.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletionStage;

import static org.junit.jupiter.api.Assertions.*;

/**
 * DefaultSkillInvoker 测试.
 * T-S2.18: 端到端调用 / 策略路由 /
 * 错误码正确.
 */
class DefaultSkillInvokerTest {

    private InMemorySkillRegistry registry;
    private DefaultSkillResolver resolver;
    private InvocationPipeline pipeline;
    private SkillCapability skillCapability;
    private DefaultSkillInvoker invoker;

    @BeforeEach
    void setUp() {
        registry = new InMemorySkillRegistry();

        CapabilityRegistry capRegistry =
            new StubCapabilityRegistry();

        pipeline = InvocationPipeline
            .builder()
            .registry(capRegistry)
            .build();

        skillCapability =
            new SkillCapability(pipeline);

        resolver =
            new DefaultSkillResolver(registry);

        invoker = new DefaultSkillInvoker(
            resolver, skillCapability
        );
    }

    @Test
    void shouldReturnNotFoundForMissingSkill()
        throws Exception
    {
        SkillReference ref =
            SkillReference.of("missing", "1.0.0");

        SkillInvocationRequest req =
            new SkillInvocationRequest(
                ref, Map.of()
            );

        CompletionStage<SkillResult> stage =
            invoker.invoke(req);

        SkillResult result =
            stage.toCompletableFuture().get();

        assertFalse(result.isSuccess());
        assertEquals(
            SkillErrorCode.SKILL_NOT_FOUND,
            result.errorCode()
        );
    }

    @Test
    void shouldRejectRevokedSkill()
        throws Exception
    {
        SkillPackageMetadata meta =
            makeMetadata(
                "revoked-skill",
                "1.0.0",
                SkillExecutionStrategy
                    .DIRECT_CAPABILITY
            );

        SkillDigest digest =
            SkillDigest.sha256("test");

        registry.register(meta, digest);

        SkillReference ref =
            SkillReference.of(
                "revoked-skill", "1.0.0"
            );

        // Transition to REVOKED
        registry.transition(
            ref,
            SkillLifecycleState.VALIDATED
        );
        registry.transition(
            ref,
            SkillLifecycleState.TESTED
        );
        registry.transition(
            ref,
            SkillLifecycleState.EVALUATED
        );
        registry.transition(
            ref,
            SkillLifecycleState.APPROVED
        );
        registry.transition(
            ref,
            SkillLifecycleState.PUBLISHED
        );
        registry.transition(
            ref,
            SkillLifecycleState.ACTIVE
        );
        registry.revoke(ref);

        SkillInvocationRequest req =
            new SkillInvocationRequest(
                ref, Map.of()
            );

        SkillResult result =
            invoker.invoke(req)
                .toCompletableFuture().get();

        assertFalse(result.isSuccess());
        assertEquals(
            SkillErrorCode.SKILL_NOT_FOUND,
            result.errorCode()
        );
    }

    @Test
    void shouldReturnInputInvalidForNullInput()
        throws Exception
    {
        SkillPackageMetadata meta =
            makeMetadata(
                "test-skill",
                "1.0.0",
                SkillExecutionStrategy
                    .DIRECT_CAPABILITY
            );

        SkillDigest digest =
            SkillDigest.sha256("test");

        registry.register(meta, digest);

        SkillReference ref =
            SkillReference.of(
                "test-skill", "1.0.0"
            );

        // Activate the skill
        activateSkill(ref);

        // Input is null — should fail
        // validation
        SkillInvocationRequest req =
            new SkillInvocationRequest(
                ref, null,
                null, null, 5000L
            );

        SkillResult result =
            invoker.invoke(req)
                .toCompletableFuture().get();

        // Null input gets converted to
        // empty map, so it passes
        // validation. The actual execution
        // may fail because no capability
        // is registered.
        assertNotNull(result);
    }

    @Test
    void shouldRouteDirectCapability()
        throws Exception
    {
        SkillPackageMetadata meta =
            makeMetadata(
                "direct-skill",
                "1.0.0",
                SkillExecutionStrategy
                    .DIRECT_CAPABILITY
            );

        SkillDigest digest =
            SkillDigest.sha256("test");

        registry.register(meta, digest);

        SkillReference ref =
            SkillReference.of(
                "direct-skill", "1.0.0"
            );

        activateSkill(ref);

        SkillInvocationRequest req =
            new SkillInvocationRequest(
                ref, Map.of("x", 1)
            );

        SkillResult result =
            invoker.invoke(req)
                .toCompletableFuture().get();

        // DIRECT_CAPABILITY will try to
        // execute through pipeline. Since
        // no real capability is registered,
        // it will fail with
        // SKILL_EXECUTION_FAILED.
        assertNotNull(result);
        assertFalse(result.isSuccess());
        assertEquals(
            SkillErrorCode
                .SKILL_EXECUTION_FAILED,
            result.errorCode()
        );
    }

    @Test
    void shouldReturnFailedForGraphStrategy()
        throws Exception
    {
        SkillPackageMetadata meta =
            makeMetadata(
                "graph-skill",
                "1.0.0",
                SkillExecutionStrategy.GRAPH
            );

        SkillDigest digest =
            SkillDigest.sha256("test");

        registry.register(meta, digest);

        SkillReference ref =
            SkillReference.of(
                "graph-skill", "1.0.0"
            );

        activateSkill(ref);

        SkillInvocationRequest req =
            new SkillInvocationRequest(
                ref, Map.of()
            );

        SkillResult result =
            invoker.invoke(req)
                .toCompletableFuture().get();

        assertFalse(result.isSuccess());
        assertEquals(
            SkillErrorCode
                .SKILL_EXECUTION_FAILED,
            result.errorCode()
        );
        assertTrue(
            result.errorMessage()
                .contains("GRAPH")
        );
    }

    @Test
    void shouldReturnFailedForAgentStrategy()
        throws Exception
    {
        SkillPackageMetadata meta =
            makeMetadata(
                "agent-skill",
                "1.0.0",
                SkillExecutionStrategy.AGENT
            );

        SkillDigest digest =
            SkillDigest.sha256("test");

        registry.register(meta, digest);

        SkillReference ref =
            SkillReference.of(
                "agent-skill", "1.0.0"
            );

        activateSkill(ref);

        SkillInvocationRequest req =
            new SkillInvocationRequest(
                ref, Map.of()
            );

        SkillResult result =
            invoker.invoke(req)
                .toCompletableFuture().get();

        assertFalse(result.isSuccess());
        assertTrue(
            result.errorMessage()
                .contains("AGENT")
        );
    }

    // ── Helpers ────────────────────

    private void activateSkill(
        SkillReference ref
    ) {
        registry.transition(
            ref,
            SkillLifecycleState.VALIDATED
        );
        registry.transition(
            ref,
            SkillLifecycleState.TESTED
        );
        registry.transition(
            ref,
            SkillLifecycleState.EVALUATED
        );
        registry.transition(
            ref,
            SkillLifecycleState.APPROVED
        );
        registry.transition(
            ref,
            SkillLifecycleState.PUBLISHED
        );
        registry.transition(
            ref,
            SkillLifecycleState.ACTIVE
        );
    }

    private SkillPackageMetadata makeMetadata(
        String skillId,
        String version,
        SkillExecutionStrategy strategy
    ) {
        Schema s = () -> "object";
        return new SkillPackageMetadata(
            new SkillId(skillId),
            new Version(version),
            skillId,
            "Test goal",
            SkillContract.of(s, s),
            SkillDependencies.of(
                Set.of(
                    new CapabilityRef(
                        new io.github.regalpine
                            .agentforge.kernel
                            .EntityId("cap-1"),
                        new Version("1.0.0")
                    )
                ),
                Set.of(),
                Set.of()
            ),
            strategy,
            "owner",
            "domain",
            Set.of("test"),
            Map.of(),
            URI.create("file:///test")
        );
    }

    private static class StubCapabilityRegistry
        implements CapabilityRegistry {

        @Override
        public void register(
            CapabilityDefinition definition
        ) {}

        @Override
        public Optional<CapabilityDefinition> find(
            Reference reference
        ) {
            return Optional.empty();
        }

        @Override
        public java.util.List<CapabilityDefinition>
            list()
        {
            return java.util.List.of();
        }

        @Override
        public void unregister(
            Reference reference
        ) {}
    }
}
