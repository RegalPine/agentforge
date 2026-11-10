package io.github.regalpine.agentforge.capability;

import io.github.regalpine.agentforge.kernel.*;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

/**
 * SkillCapability 测试.
 * T-S0.16: Skill → Capability 调用桥接正确、
 * Pipeline 完整经过.
 */
class SkillCapabilityTest {

    @Test
    void shouldRejectNonDirectCapabilityStrategy() {
        // Build a minimal pipeline
        InvocationPipeline pipeline =
            InvocationPipeline.builder()
                .registry(new StubRegistry())
                .build();

        SkillCapability adapter =
            new SkillCapability(pipeline);

        // Create a Skill with GRAPH strategy
        Schema s = () -> "object";
        Skill skill = new SkillDefinition(
            new SkillId("graph-skill"),
            new Version("1.0.0"),
            "Graph Skill",
            "Uses graph",
            SkillContract.of(s, s),
            SkillDependencies.empty(),
            SkillExecutionStrategy.GRAPH
        );

        InvocationContext ctx =
            new InvocationContext(
                "exec-1", "t1", Map.of()
            );

        assertThrows(
            IllegalArgumentException.class,
            () -> adapter.invoke(
                skill, Map.of(), ctx
            )
        );
    }

    @Test
    void shouldRejectSkillWithNoCapabilityDeps() {
        InvocationPipeline pipeline =
            InvocationPipeline.builder()
                .registry(new StubRegistry())
                .build();

        SkillCapability adapter =
            new SkillCapability(pipeline);

        // DIRECT_CAPABILITY but no deps
        Schema s = () -> "object";
        Skill skill = SkillDefinition.of(
            "empty-skill", "1.0.0",
            "Empty", "No deps",
            SkillContract.of(s, s)
        );

        InvocationContext ctx =
            new InvocationContext(
                "exec-1", "t1", Map.of()
            );

        assertThrows(
            IllegalStateException.class,
            () -> adapter.invoke(
                skill, Map.of(), ctx
            )
        );
    }

    @Test
    void shouldRejectNullSkill() {
        InvocationPipeline pipeline =
            InvocationPipeline.builder()
                .registry(new StubRegistry())
                .build();

        SkillCapability adapter =
            new SkillCapability(pipeline);

        InvocationContext ctx =
            new InvocationContext(
                "exec-1", "t1", Map.of()
            );

        assertThrows(
            NullPointerException.class,
            () -> adapter.invoke(
                null, Map.of(), ctx
            )
        );
    }

    @Test
    void shouldRejectNullPipeline() {
        assertThrows(
            NullPointerException.class,
            () -> new SkillCapability(null)
        );
    }

    /**
     * Stub registry for test — always empty.
     */
    private static class StubRegistry
        implements CapabilityRegistry {

        @Override
        public void register(
            CapabilityDefinition definition
        ) {
            // no-op
        }

        @Override
        public Optional<CapabilityDefinition> find(
            Reference reference
        ) {
            return Optional.empty();
        }

        @Override
        public java.util.List<CapabilityDefinition> list() {
            return java.util.List.of();
        }

        @Override
        public void unregister(
            Reference reference
        ) {
            // no-op
        }
    }
}
