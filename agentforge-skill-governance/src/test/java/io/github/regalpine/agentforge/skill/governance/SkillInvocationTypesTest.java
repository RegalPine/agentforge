package io.github.regalpine.agentforge.skill.governance;

import io.github.regalpine.agentforge.kernel.Schema;
import io.github.regalpine.agentforge.kernel.Skill;
import io.github.regalpine.agentforge.kernel.SkillContract;
import io.github.regalpine.agentforge.kernel.SkillDefinition;
import io.github.regalpine.agentforge.kernel.SkillDependencies;
import io.github.regalpine.agentforge.kernel.SkillExecutionStrategy;
import io.github.regalpine.agentforge.kernel.SkillId;
import io.github.regalpine.agentforge.kernel.SkillInvocationRequest;
import io.github.regalpine.agentforge.kernel.SkillReference;
import io.github.regalpine.agentforge.kernel.Version;
import io.github.regalpine.agentforge.kernel.ResolvedSkill;


import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Skill 调用类型测试.
 * T-S2.18: SkillInvocationRequest /
 * SkillResult / ResolvedSkill /
 * SkillResultStatus.
 */
class SkillInvocationTypesTest {

    // ── SkillInvocationRequest ──────

    @Test
    void shouldCreateRequest() {
        SkillReference ref =
            SkillReference.of("s1", "1.0.0");

        SkillInvocationRequest req =
            new SkillInvocationRequest(
                ref,
                Map.of("key", "val")
            );

        assertEquals("s1",
            req.skill().skillId().value());
        assertEquals("val",
            req.input().get("key"));
        assertEquals(30_000L,
            req.timeoutMillis());
        assertTrue(req.callerIdOpt().isEmpty());
        assertTrue(req.tenantIdOpt().isEmpty());
    }

    @Test
    void shouldCreateFullRequest() {
        SkillReference ref =
            SkillReference.of("s1", "1.0.0");

        SkillInvocationRequest req =
            new SkillInvocationRequest(
                ref,
                Map.of("data", 42),
                "caller-1",
                "tenant-1",
                60_000L
            );

        assertEquals("caller-1",
            req.callerIdOpt().orElseThrow());
        assertEquals("tenant-1",
            req.tenantIdOpt().orElseThrow());
        assertEquals(60_000L,
            req.timeoutMillis());
    }

    @Test
    void shouldRejectNullSkill() {
        assertThrows(
            NullPointerException.class,
            () -> new SkillInvocationRequest(
                null, Map.of()
            )
        );
    }

    @Test
    void shouldRejectNegativeTimeout() {
        SkillReference ref =
            SkillReference.of("s1", "1.0.0");

        assertThrows(
            IllegalArgumentException.class,
            () -> new SkillInvocationRequest(
                ref, Map.of(),
                null, null, -1
            )
        );
    }

    @Test
    void shouldDefensivelyCopyInput() {
        SkillReference ref =
            SkillReference.of("s1", "1.0.0");

        Map<String, Object> input =
            new java.util.HashMap<>();
        input.put("a", 1);

        SkillInvocationRequest req =
            new SkillInvocationRequest(
                ref, input,
                null, null, 5000L
            );

        input.put("b", 2);
        assertNull(req.input().get("b"));
    }

    @Test
    void shouldHandleNullInputAsEmpty() {
        SkillReference ref =
            SkillReference.of("s1", "1.0.0");

        SkillInvocationRequest req =
            new SkillInvocationRequest(
                ref, null,
                null, null, 5000L
            );

        assertTrue(req.input().isEmpty());
    }

    // ── SkillResult ────────────────

    @Test
    void shouldCreateSuccessResult() {
        SkillReference ref =
            SkillReference.of("s1", "1.0.0");

        SkillResult result =
            SkillResult.success(
                ref,
                Map.of("output", "done")
            );

        assertTrue(result.isSuccess());
        assertEquals(
            SkillResultStatus.COMPLETED,
            result.status()
        );
        assertEquals("done",
            result.output().get("output"));
        assertTrue(result.errorCodeOpt().isEmpty());
    }

    @Test
    void shouldCreateFailedResult() {
        SkillReference ref =
            SkillReference.of("s1", "1.0.0");

        SkillResult result =
            SkillResult.failed(
                ref,
                SkillErrorCode.SKILL_TIMEOUT,
                "timed out"
            );

        assertFalse(result.isSuccess());
        assertEquals(
            SkillResultStatus.FAILED,
            result.status()
        );
        assertEquals(
            SkillErrorCode.SKILL_TIMEOUT,
            result.errorCodeOpt().orElseThrow()
        );
        assertEquals("timed out",
            result.errorMessage());
    }

    @Test
    void shouldRejectNullSkillInResult() {
        assertThrows(
            NullPointerException.class,
            () -> SkillResult.success(
                null, Map.of()
            )
        );
    }

    // ── ResolvedSkill ──────────────

    @Test
    void shouldCreateResolvedSkill() {
        SkillId id = new SkillId("test");
        Version v = new Version("1.0.0");
        Schema s = () -> "object";
        SkillContract c =
            SkillContract.of(s, s);
        SkillDependencies d =
            SkillDependencies.empty();

        Skill skill = new SkillDefinition(
            id, v, "Test", "goal",
            c, d,
            SkillExecutionStrategy
                .DIRECT_CAPABILITY
        );

        SkillReference ref =
            SkillReference.of("test", "1.0.0");

        ResolvedSkill resolved =
            new ResolvedSkill(
                skill, ref,
                SkillExecutionStrategy
                    .DIRECT_CAPABILITY
            );

        assertEquals("Test",
            resolved.skill().name());
        assertEquals(
            SkillExecutionStrategy
                .DIRECT_CAPABILITY,
            resolved.strategy()
        );
    }

    @Test
    void shouldRejectNullSkillInResolved() {
        SkillReference ref =
            SkillReference.of("s", "1.0.0");

        assertThrows(
            IllegalArgumentException.class,
            () -> new ResolvedSkill(
                null, ref,
                SkillExecutionStrategy.GRAPH
            )
        );
    }

    // ── SkillResultStatus ──────────

    @Test
    void shouldHaveAllStatuses() {
        assertEquals(5,
            SkillResultStatus.values().length
        );
        assertNotNull(
            SkillResultStatus.COMPLETED
        );
        assertNotNull(
            SkillResultStatus.FAILED
        );
        assertNotNull(
            SkillResultStatus.TIMEOUT
        );
        assertNotNull(
            SkillResultStatus.CANCELLED
        );
        assertNotNull(
            SkillResultStatus.WAITING_HUMAN
        );
    }
}
