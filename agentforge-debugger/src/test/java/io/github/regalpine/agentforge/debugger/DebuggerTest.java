package io.github.regalpine.agentforge.debugger;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Debugger 核心测试。
 */
class DebuggerTest {

    @Test
    void shouldCreateDebugSession() {
        var session =
            new DebugSession("exec-1");

        assertEquals(
            "exec-1",
            session.executionId()
        );
        assertEquals(
            DebugSessionStatus.CREATED,
            session.status()
        );
        assertNotNull(session.id());
    }

    @Test
    void shouldAttachAndResume() {
        var session =
            new DebugSession("exec-1");

        session.attach();
        assertEquals(
            DebugSessionStatus.ATTACHED,
            session.status()
        );

        session.resume();
        assertEquals(
            DebugSessionStatus.RUNNING,
            session.status()
        );
    }

    @Test
    void shouldAddAndTriggerBreakpoint() {
        var session =
            new DebugSession("exec-1");
        session.attach();
        session.resume();

        Breakpoint bp =
            Breakpoint.of(
                BreakpointType.TOOL,
                "web-search"
            );
        session.addBreakpoint(bp);

        assertEquals(
            1,
            session.getBreakpoints().size()
        );

        // 匹配断点
        var ctx = DebugContext.builder()
            .executionId("exec-1")
            .eventType("tool.started")
            .toolId("web-search")
            .build();

        Breakpoint hit =
            session.checkBreakpoints(ctx);

        assertNotNull(hit);
        assertEquals(
            DebugSessionStatus.PAUSED,
            session.status()
        );
    }

    @Test
    void shouldNotTriggerDisabledBp() {
        var session =
            new DebugSession("exec-1");
        session.attach();
        session.resume();

        Breakpoint bp =
            Breakpoint.of(
                BreakpointType.TOOL
            ).withEnabled(false);
        session.addBreakpoint(bp);

        var ctx = DebugContext.of(
            "exec-1",
            "tool.started"
        );

        Breakpoint hit =
            session.checkBreakpoints(ctx);
        assertNull(hit);
    }

    @Test
    void shouldConditionalBreakpoint() {
        var session =
            new DebugSession("exec-1");
        session.attach();
        session.resume();

        Breakpoint bp =
            Breakpoint.conditional(
                BreakpointType.TOOL,
                BreakpointCondition
                    .attributeEquals(
                        "tool",
                        "payment"
                    )
            );
        session.addBreakpoint(bp);

        // 不匹配
        var ctx1 = DebugContext.builder()
            .executionId("exec-1")
            .eventType("tool.started")
            .attributes(
                Map.of("tool", "search")
            )
            .build();
        assertNull(
            session.checkBreakpoints(ctx1)
        );

        // 匹配
        var ctx2 = DebugContext.builder()
            .executionId("exec-1")
            .eventType("tool.started")
            .attributes(
                Map.of("tool", "payment")
            )
            .build();
        assertNotNull(
            session.checkBreakpoints(ctx2)
        );
    }

    @Test
    void shouldStepControl() {
        var session =
            new DebugSession("exec-1");
        session.attach();
        session.resume();
        session.pause();

        assertEquals(
            DebugSessionStatus.PAUSED,
            session.status()
        );

        session.step(
            StepCommand.STEP_OVER
        );
        // 仍然 PAUSED（等待下一步）
        assertEquals(
            DebugSessionStatus.PAUSED,
            session.status()
        );

        session.step(
            StepCommand.CONTINUE
        );
        assertEquals(
            DebugSessionStatus.RUNNING,
            session.status()
        );
    }

    @Test
    void shouldRejectStepWhenNotPaused() {
        var session =
            new DebugSession("exec-1");
        session.attach();
        session.resume();

        assertThrows(
            IllegalStateException.class,
            () -> session.step(
                StepCommand.STEP_INTO
            )
        );
    }

    @Test
    void shouldUpdateCursor() {
        var session =
            new DebugSession("exec-1");

        var cursor =
            ExecutionCursor.of(
                "evt-1", 5,
                CursorType.TOOL
            );
        session.updateCursor(cursor);

        assertEquals(
            cursor,
            session.cursor()
        );
    }

    @Test
    void shouldTrackOverrides() {
        var session =
            new DebugSession("exec-1");

        session.addOverride(
            DebugOverride.of(
                "tool.output",
                "mocked-result",
                "Testing"
            )
        );

        assertEquals(
            1,
            session.overrides().size()
        );
    }

    @Test
    void shouldAllBreakpointTypes() {
        for (BreakpointType type
            : BreakpointType.values()
        ) {
            Breakpoint bp =
                Breakpoint.of(type);
            assertNotNull(bp.id());
            assertEquals(type, bp.type());
            assertTrue(bp.enabled());
        }
    }

    @Test
    void shouldAllStepCommands() {
        assertEquals(
            7,
            StepCommand.values().length
        );
    }

    @Test
    void shouldDebugSessionStatus() {
        assertTrue(
            DebugSessionStatus.RUNNING
                .isActive()
        );
        assertTrue(
            DebugSessionStatus.PAUSED
                .isActive()
        );
        assertTrue(
            DebugSessionStatus.COMPLETED
                .isTerminal()
        );
        assertFalse(
            DebugSessionStatus.RUNNING
                .isTerminal()
        );
    }
}
