package io.github.regalpine.agentforge.hardening.conformance;

import io.github.regalpine.agentforge.debugger.*;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * conformance-debugger-testkit:
 * DebugSession / Breakpoint /
 * StepCommand / TestKit.
 * AF-AD-16 §6.4, T-X12.4.
 */
class DebuggerConformanceTest {

    // ── DebugSession ──

    @Test
    void afDbg001_sessionCreation() {
        var s = new DebugSession(
            "exec-1"
        );
        assertNotNull(s.id());
        assertEquals(
            "exec-1", s.executionId()
        );
        assertEquals(
            DebugSessionStatus.CREATED,
            s.status()
        );
        assertNotNull(
            s.createdAt()
        );
    }

    @Test
    void afDbg002_sessionLifecycle() {
        var s = new DebugSession(
            "exec-1"
        );
        s.attach();
        assertEquals(
            DebugSessionStatus.ATTACHED,
            s.status()
        );
        assertTrue(
            s.status().isActive()
        );

        s.resume();
        assertEquals(
            DebugSessionStatus.RUNNING,
            s.status()
        );

        s.pause();
        assertEquals(
            DebugSessionStatus.PAUSED,
            s.status()
        );

        s.complete();
        assertTrue(
            s.status().isTerminal()
        );
    }

    @Test
    void afDbg003_sessionReplay() {
        var s = new DebugSession(
            "exec-1"
        );
        s.replay();
        assertEquals(
            DebugSessionStatus.REPLAYING,
            s.status()
        );
        assertTrue(
            s.status().isActive()
        );
    }

    @Test
    void afDbg004_sessionTerminate() {
        var s = new DebugSession(
            "exec-1"
        );
        s.terminate();
        assertEquals(
            DebugSessionStatus.TERMINATED,
            s.status()
        );
        assertTrue(
            s.status().isTerminal()
        );
    }

    // ── Breakpoint ──

    @Test
    void afDbg005_breakpointCreation() {
        var bp = Breakpoint.of(
            BreakpointType.EXECUTION
        );
        assertNotNull(bp.id());
        assertEquals(
            BreakpointType.EXECUTION,
            bp.type()
        );
        assertTrue(bp.enabled());
    }

    @Test
    void afDbg006_breakpointWithTarget() {
        var bp = Breakpoint.of(
            BreakpointType.SKILL,
            "skill-1"
        );
        assertEquals(
            "skill-1", bp.targetId()
        );
        assertEquals(
            BreakpointType.SKILL,
            bp.type()
        );
    }

    @Test
    void afDbg007_conditionalBreakpoint() {
        var cond = BreakpointCondition
            .attributeEquals(
                "key", "value"
            );
        var bp = Breakpoint.conditional(
            BreakpointType.MODEL,
            cond
        );
        assertTrue(bp.enabled());
        assertEquals(
            BreakpointType.MODEL,
            bp.type()
        );
    }

    @Test
    void afDbg008_breakpointEnableDisable() {
        var bp = Breakpoint.of(
            BreakpointType.TOOL
        );
        assertTrue(bp.enabled());
        var disabled = bp.withEnabled(
            false
        );
        assertFalse(
            disabled.enabled()
        );
    }

    // ── BreakpointType ──

    @Test
    void afDbg009_tenBreakpointTypes() {
        assertEquals(
            10,
            BreakpointType.values().length
        );
        assertNotNull(
            BreakpointType.EXECUTION
        );
        assertNotNull(
            BreakpointType.PLAN_STEP
        );
        assertNotNull(
            BreakpointType.SKILL
        );
        assertNotNull(
            BreakpointType.TOOL
        );
        assertNotNull(
            BreakpointType.MODEL
        );
        assertNotNull(
            BreakpointType.MEMORY
        );
        assertNotNull(
            BreakpointType.POLICY
        );
        assertNotNull(
            BreakpointType.APPROVAL
        );
        assertNotNull(
            BreakpointType.EVENT
        );
        assertNotNull(
            BreakpointType.ERROR
        );
    }

    // ── StepCommand ──

    @Test
    void afDbg010_sevenStepCommands() {
        assertEquals(
            7,
            StepCommand.values().length
        );
        assertNotNull(
            StepCommand.CONTINUE
        );
        assertNotNull(
            StepCommand.PAUSE
        );
        assertNotNull(
            StepCommand.STEP_OVER
        );
        assertNotNull(
            StepCommand.STEP_INTO
        );
        assertNotNull(
            StepCommand.STEP_OUT
        );
        assertNotNull(
            StepCommand.RESTART
        );
        assertNotNull(
            StepCommand.STOP
        );
    }

    // ── DebugSessionStatus ──

    @Test
    void afDbg011_sevenSessionStatuses() {
        assertEquals(
            7,
            DebugSessionStatus.values()
                .length
        );
        assertTrue(
            DebugSessionStatus.ATTACHED
                .isActive()
        );
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
    }

    // ── BreakpointCondition ──

    @Test
    void afDbg012_conditionFactories() {
        var always = BreakpointCondition
            .always();
        assertNotNull(always);

        var never = BreakpointCondition
            .never();
        assertNotNull(never);

        var attr = BreakpointCondition
            .attributeEquals(
                "k", "v"
            );
        assertNotNull(attr);
    }

    // ── ExecutionCursor ──

    @Test
    void afDbg013_cursorCreation() {
        var c = ExecutionCursor.of(
            "evt-1", 42L,
            CursorType.EXECUTION
        );
        assertEquals(
            "evt-1", c.eventId()
        );
        assertEquals(42L,
            c.sequence());
        assertEquals(
            CursorType.EXECUTION,
            c.type()
        );
    }

    // ── Step Execution ──

    @Test
    void afDbg014_stepRequiresPaused() {
        var s = new DebugSession(
            "exec-1"
        );
        s.attach();
        s.resume();
        // Not paused — should throw
        assertThrows(
            IllegalStateException.class,
            () -> s.step(
                StepCommand.STEP_OVER
            )
        );
    }

    @Test
    void afDbg015_breakpointManagement() {
        var s = new DebugSession(
            "exec-1"
        );
        var bp = Breakpoint.of(
            BreakpointType.EXECUTION
        );
        s.addBreakpoint(bp);
        assertEquals(1,
            s.getBreakpoints().size()
        );
        s.removeBreakpoint(bp.id());
        assertTrue(
            s.getBreakpoints().isEmpty()
        );
    }
}
