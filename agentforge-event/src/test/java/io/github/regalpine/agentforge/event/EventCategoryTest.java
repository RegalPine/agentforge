package io.github.regalpine.agentforge.event;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * EventCategory 测试。
 */
class EventCategoryTest {

    @Test
    void shouldMapAllCategories() {
        assertEquals(
            EventCategory.LIFECYCLE,
            EventCategory.fromEventType("agent.started")
        );
        assertEquals(
            EventCategory.EXECUTION,
            EventCategory.fromEventType(
                "execution.completed"
            )
        );
        assertEquals(
            EventCategory.SKILL,
            EventCategory.fromEventType("skill.failed")
        );
        assertEquals(
            EventCategory.TOOL,
            EventCategory.fromEventType("tool.started")
        );
        assertEquals(
            EventCategory.MODEL,
            EventCategory.fromEventType("model.delta")
        );
        assertEquals(
            EventCategory.MESSAGE,
            EventCategory.fromEventType(
                "message.created"
            )
        );
        assertEquals(
            EventCategory.PLAN,
            EventCategory.fromEventType("plan.created")
        );
        assertEquals(
            EventCategory.MEMORY,
            EventCategory.fromEventType(
                "memory.read.started"
            )
        );
        assertEquals(
            EventCategory.POLICY,
            EventCategory.fromEventType(
                "policy.denied"
            )
        );
        assertEquals(
            EventCategory.HUMAN,
            EventCategory.fromEventType(
                "human.approval.required"
            )
        );
        assertEquals(
            EventCategory.ERROR,
            EventCategory.fromEventType(
                "error.occurred"
            )
        );
    }

    @Test
    void shouldMapSecurityToError() {
        assertEquals(
            EventCategory.ERROR,
            EventCategory.fromEventType(
                "security.violation"
            )
        );
    }

    @Test
    void shouldRejectNull() {
        assertThrows(
            IllegalArgumentException.class,
            () -> EventCategory.fromEventType(null)
        );
    }
}
