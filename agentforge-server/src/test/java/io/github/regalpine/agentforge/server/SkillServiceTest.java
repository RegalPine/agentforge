package io.github.regalpine.agentforge.server;

import io.github.regalpine.agentforge.capability.InMemorySkillRegistry;
import io.github.regalpine.agentforge.kernel.*;
import io.github.regalpine.agentforge.skill.governance.*;
import io.github.regalpine.agentforge.server.service.SkillService;
import io.github.regalpine.agentforge.runtime.SkillInvoker;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Server Skill REST API 测试.
 * T-S5.26: CRUD / 生命周期 / 调用.
 */
class SkillServiceTest {

    private InMemorySkillRegistry registry;
    private SkillInvoker invoker;
    private SkillService service;

    @BeforeEach
    void setUp() {
        registry = new InMemorySkillRegistry();
        invoker = request ->
            CompletableFuture.completedFuture(
                SkillResult.success(
                    request.skill(),
                    Map.of("result", "ok")
                )
            );
        service = new SkillService(
            registry, invoker
        );

        // Register a test skill
        Schema emptySchema = () -> "object";
        SkillContract contract =
            SkillContract.of(emptySchema, emptySchema);

        SkillPackageMetadata metadata =
            new SkillPackageMetadata(
                new SkillId("test-skill"),
                new Version("1.0.0"),
                "Test Skill",
                "Test goal",
                contract,
                SkillDependencies.empty(),
                SkillExecutionStrategy.DIRECT_CAPABILITY,
                "owner", "domain",
                null, null,
                URI.create("file:///test.tar.gz")
            );

        registry.register(
            metadata,
            SkillDigest.sha256("test")
        );
    }

    @Test
    void shouldListSkills() {
        List<SkillDescriptor> skills =
            service.listSkills();

        assertFalse(skills.isEmpty());
        assertEquals(
            "test-skill",
            skills.get(0).skillId().value()
        );
    }

    @Test
    void shouldGetSkillById() {
        var opt = service.getSkill("test-skill");

        assertTrue(opt.isPresent());
        assertEquals(
            "Test Skill",
            opt.get().name()
        );
    }

    @Test
    void shouldReturnEmptyForUnknownSkill() {
        var opt = service.getSkill("unknown");
        assertTrue(opt.isEmpty());
    }

    @Test
    void shouldGetSkillVersions() {
        var versions =
            service.getSkillVersions("test-skill");

        assertFalse(versions.isEmpty());
        assertEquals(
            "1.0.0",
            versions.get(0).version().value()
        );
    }

    @Test
    void shouldGetLifecycleState() {
        var state = service.getState(
            "test-skill", "1.0.0"
        );

        assertNotNull(state);
        assertEquals(
            SkillLifecycleState.DRAFT,
            state
        );
    }

    @Test
    void shouldTransitionLifecycle() {
        // DRAFT → VALIDATED (first step)
        service.transition(
            "test-skill", "1.0.0",
            SkillLifecycleState.VALIDATED
        );

        var state = service.getState(
            "test-skill", "1.0.0"
        );
        assertEquals(
            SkillLifecycleState.VALIDATED,
            state
        );
    }

    @Test
    void shouldInvokeSkill() {
        var result = service.invoke(
            "test-skill", "1.0.0",
            Map.of("input", "data"),
            "tenant-1"
        ).toCompletableFuture().join();

        assertNotNull(result);
        assertTrue(result.isSuccess());
        assertEquals(
            "ok",
            result.output().get("result")
        );
    }

    @Test
    void shouldHandleNullInput() {
        var result = service.invoke(
            "test-skill", "1.0.0",
            null, "tenant-1"
        ).toCompletableFuture().join();

        assertTrue(result.isSuccess());
    }

    @Test
    void shouldDiscoverSkills() {
        var results = service.discoverSkills(
            SkillQuery.all()
        );

        assertFalse(results.isEmpty());
    }
}
