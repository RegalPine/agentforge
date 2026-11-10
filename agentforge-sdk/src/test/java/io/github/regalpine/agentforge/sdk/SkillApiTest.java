package io.github.regalpine.agentforge.sdk;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * SDK Skill 集成测试.
 * T-S5.24: SkillClient / Facade /
 * 不暴露内部.
 */
class SkillApiTest {

    // ── SkillDescriptor ───────

    @Test
    void shouldCreateSdkDescriptor() {
        var kernel =
            new io.github.regalpine.agentforge.kernel.SkillDescriptor(
                new io.github.regalpine.agentforge.kernel.SkillId("s1"),
                "TestSkill",
                new io.github.regalpine.agentforge.kernel.Version("1.0.0"),
                "Test goal",
                io.github.regalpine.agentforge.kernel.SkillExecutionStrategy.DIRECT_CAPABILITY,
                "owner1",
                "ACTIVE"
            );

        var sdk = SkillDescriptor.from(kernel);

        assertEquals("s1", sdk.skillId());
        assertEquals("TestSkill", sdk.name());
        assertEquals("1.0.0", sdk.version());
        assertEquals("Test goal", sdk.goal());
        assertEquals(
            "DIRECT_CAPABILITY",
            sdk.strategy()
        );
        assertEquals("owner1", sdk.owner());
        assertEquals("ACTIVE", sdk.status());
    }

    // ── SkillDiscoveryQuery ───

    @Test
    void shouldCreateAllQuery() {
        var q = SkillDiscoveryQuery.all();

        assertNull(q.idPattern());
        assertNull(q.owner());
        assertNull(q.status());
    }

    @Test
    void shouldCreateByIdQuery() {
        var q = SkillDiscoveryQuery.byId("s1");

        assertEquals("s1", q.idPattern());
        assertNull(q.owner());

        var kq = q.toKernelQuery();
        assertNotNull(kq);
        assertEquals("s1", kq.idPattern());
    }

    @Test
    void shouldCreateByOwnerQuery() {
        var q = SkillDiscoveryQuery.byOwner("alice");

        assertEquals("alice", q.owner());
        assertNull(q.idPattern());
    }

    // ── SkillExecutionResult ──

    @Test
    void shouldCreateSuccessResult() {
        var r = SkillExecutionResult.success(
            "s1", "1.0.0",
            Map.of("key", "value")
        );

        assertEquals("s1", r.skillId());
        assertEquals("1.0.0", r.version());
        assertEquals(
            SkillExecutionResult.Status.SUCCESS,
            r.status()
        );
        assertEquals("value", r.output().get("key"));
        assertNull(r.errorCode());
        assertNull(r.errorMessage());
        assertNotNull(r.completedAt());
    }

    @Test
    void shouldCreateFailureResult() {
        var r = SkillExecutionResult.failure(
            "s1", "1.0.0",
            "ERR_001", "Something failed"
        );

        assertEquals(
            SkillExecutionResult.Status.FAILED,
            r.status()
        );
        assertEquals("ERR_001", r.errorCode());
        assertEquals("Something failed", r.errorMessage());
        assertTrue(r.output().isEmpty());
    }

    @Test
    void shouldDefensivelyCopyOutput() {
        var r = SkillExecutionResult.success(
            "s1", "1.0.0",
            Map.of("a", "b")
        );

        assertNotNull(r.output());
        assertEquals(1, r.output().size());
    }

    @Test
    void shouldHandleNullOutput() {
        var r = new SkillExecutionResult(
            "s1", "1.0.0",
            SkillExecutionResult.Status.SUCCESS,
            null, null, null, null
        );

        assertNotNull(r.output());
        assertTrue(r.output().isEmpty());
        assertNotNull(r.completedAt());
    }

    @Test
    void shouldConvertFromKernelResult() {
        var ref = io.github.regalpine.agentforge.kernel.SkillReference.of(
            "s1", "1.0.0"
        );
        var kernel = io.github.regalpine.agentforge.skill.governance.SkillResult.success(
            ref, Map.of("result", 42)
        );

        var sdk = SkillExecutionResult.from(kernel);

        assertEquals("s1", sdk.skillId());
        assertEquals("1.0.0", sdk.version());
        assertEquals(
            SkillExecutionResult.Status.SUCCESS,
            sdk.status()
        );
        assertEquals(42, sdk.output().get("result"));
    }

    // ── NoOpSkillApi ──────────

    @Test
    void noOpShouldReturnEmptyList() {
        var api = new NoOpSkillApi();

        assertTrue(api.listSkillIds().isEmpty());
        assertTrue(
            api.discover(SkillDiscoveryQuery.all())
                .isEmpty()
        );
    }

    @Test
    void noOpShouldThrowOnResolve() {
        var api = new NoOpSkillApi();

        assertThrows(
            ResolutionException.class,
            () -> api.resolve("s1", "1.0.0")
        );
    }

    @Test
    void noOpShouldReturnFailureOnInvoke() {
        var api = new NoOpSkillApi();

        var result = api.invoke(
            "s1", "1.0.0", Map.of()
        );

        assertEquals(
            SkillExecutionResult.Status.FAILED,
            result.status()
        );
        assertEquals(
            "SKILL_NOT_CONFIGURED",
            result.errorCode()
        );
    }

    // ── AgentForge.skills() ────

    @Test
    void agentForgeShouldExposeSkillsApi() {
        try (var forge = AgentForge.bootstrap()) {
            SkillApi api = forge.skills();
            assertNotNull(api);
            assertTrue(api.listSkillIds().isEmpty());
        }
    }
}
