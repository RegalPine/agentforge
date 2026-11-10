package io.github.regalpine.agentforge.graph.ir;

import io.github.regalpine.agentforge.kernel.SkillReference;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * SkillNode 测试.
 * T-S0.17: 节点构造 / IR 序列化 round-trip.
 */
class SkillNodeTest {

    @Test
    void shouldCreateSkillNode() {
        SkillReference ref = SkillReference.of(
            "customer-analysis", "1.0.0"
        );

        SkillNode node = new SkillNode(
            ref,
            Map.of("input", "$.data"),
            Map.of("output", "$.result")
        );

        assertEquals(
            "customer-analysis",
            node.skill().skillId().value()
        );
        assertEquals(
            "$.data",
            node.inputMapping().get("input")
        );
        assertEquals(
            "$.result",
            node.outputMapping().get("output")
        );
    }

    @Test
    void shouldDefaultNullMappingsToEmpty() {
        SkillReference ref = SkillReference.of(
            "test-skill", "1.0.0"
        );

        SkillNode node = new SkillNode(
            ref, null, null
        );

        assertTrue(
            node.inputMapping().isEmpty()
        );
        assertTrue(
            node.outputMapping().isEmpty()
        );
    }

    @Test
    void shouldRejectNullSkill() {
        assertThrows(
            NullPointerException.class,
            () -> new SkillNode(
                null, Map.of(), Map.of()
            )
        );
    }

    @Test
    void shouldBeImmutable() {
        SkillReference ref = SkillReference.of(
            "test", "1.0.0"
        );

        var inputMap = new java.util.HashMap<
            String, Object
        >();
        inputMap.put("key", "value");

        SkillNode node = new SkillNode(
            ref, inputMap, Map.of()
        );

        // Modifying original map should not
        // affect the node
        inputMap.put("key2", "value2");

        assertEquals(1, node.inputMapping().size());
        assertFalse(
            node.inputMapping().containsKey("key2")
        );
    }

    @Test
    void shouldSupportEquality() {
        SkillReference ref = SkillReference.of(
            "test", "1.0.0"
        );

        SkillNode a = new SkillNode(
            ref, Map.of("k", "v"), Map.of()
        );
        SkillNode b = new SkillNode(
            ref, Map.of("k", "v"), Map.of()
        );

        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());
    }
}
