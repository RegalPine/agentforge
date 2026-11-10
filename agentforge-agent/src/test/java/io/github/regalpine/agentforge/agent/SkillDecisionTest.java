package io.github.regalpine.agentforge.agent;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * SkillDecision 测试.
 * T-S2.19: CALL_SKILL Action /
 * 类型安全 / 不可变性.
 */
class SkillDecisionTest {

    @Test
    void shouldCreateSkillDecision() {
        SkillDecision d = new SkillDecision(
            DecisionType.CALL_SKILL,
            "customer-analysis",
            "1.2.0",
            Map.of("data", "test"),
            "analyze customer"
        );

        assertEquals(
            DecisionType.CALL_SKILL,
            d.type()
        );
        assertEquals(
            "customer-analysis",
            d.skillId()
        );
        assertEquals("1.2.0", d.skillVersion());
        assertEquals(
            "test",
            d.arguments().get("data")
        );
    }

    @Test
    void shouldRejectWrongType() {
        assertThrows(
            IllegalArgumentException.class,
            () -> new SkillDecision(
                DecisionType.CALL_CAPABILITY,
                "s1", "1.0.0",
                Map.of(), "reason"
            )
        );
    }

    @Test
    void shouldRejectBlankSkillId() {
        assertThrows(
            IllegalArgumentException.class,
            () -> new SkillDecision(
                DecisionType.CALL_SKILL,
                "", "1.0.0",
                Map.of(), "reason"
            )
        );
    }

    @Test
    void shouldRejectNullSkillId() {
        assertThrows(
            IllegalArgumentException.class,
            () -> new SkillDecision(
                DecisionType.CALL_SKILL,
                null, "1.0.0",
                Map.of(), "reason"
            )
        );
    }

    @Test
    void shouldHandleNullArguments() {
        SkillDecision d = new SkillDecision(
            DecisionType.CALL_SKILL,
            "s1", "1.0.0",
            null, "reason"
        );

        assertTrue(d.arguments().isEmpty());
    }

    @Test
    void shouldDefensivelyCopyArguments() {
        Map<String, Object> args =
            new java.util.HashMap<>();
        args.put("a", 1);

        SkillDecision d = new SkillDecision(
            DecisionType.CALL_SKILL,
            "s1", "1.0.0",
            args, "reason"
        );

        args.put("b", 2);
        assertNull(d.arguments().get("b"));
    }

    @Test
    void shouldBeAgentDecision() {
        SkillDecision d = new SkillDecision(
            DecisionType.CALL_SKILL,
            "s1", "1.0.0",
            Map.of(), "reason"
        );

        assertTrue(
            d instanceof AgentDecision
        );
    }

    @Test
    void shouldHaveCallSkillInDecisionType() {
        assertEquals(5,
            DecisionType.values().length
        );
        assertNotNull(
            DecisionType.CALL_SKILL
        );
    }
}
