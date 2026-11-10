package io.github.regalpine.agentforge.agent;

import io.github.regalpine.agentforge.kernel.EntityId;
import io.github.regalpine.agentforge.kernel.Reference;
import io.github.regalpine.agentforge.kernel.Version;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for {@link AgentLifecycleState}
 * state machine transitions and
 * classification helpers.
 */
class AgentLifecycleStateTest {

    @Test
    void shouldAllowRegisteredToStarting() {
        assertTrue(
            AgentLifecycleState.REGISTERED
                .canTransitionTo(AgentLifecycleState.STARTING)
        );
    }

    @Test
    void shouldAllowStartingToActive() {
        assertTrue(
            AgentLifecycleState.STARTING
                .canTransitionTo(AgentLifecycleState.ACTIVE)
        );
    }

    @Test
    void shouldAllowActiveToDraining() {
        assertTrue(
            AgentLifecycleState.ACTIVE
                .canTransitionTo(AgentLifecycleState.DRAINING)
        );
    }

    @Test
    void shouldAllowDrainingToStopped() {
        assertTrue(
            AgentLifecycleState.DRAINING
                .canTransitionTo(AgentLifecycleState.STOPPED)
        );
    }

    @Test
    void shouldAllowAnyStateToFailed() {
        for (AgentLifecycleState s : AgentLifecycleState.values()) {
            assertTrue(
                s.canTransitionTo(AgentLifecycleState.FAILED),
                s + " should be able to transition to FAILED"
            );
        }
    }

    @Test
    void shouldRejectActiveToRegistered() {
        assertFalse(
            AgentLifecycleState.ACTIVE
                .canTransitionTo(AgentLifecycleState.REGISTERED)
        );
    }

    @Test
    void shouldRejectStoppedToActive() {
        assertFalse(
            AgentLifecycleState.STOPPED
                .canTransitionTo(AgentLifecycleState.ACTIVE)
        );
    }

    @Test
    void shouldTransitionAndReturnTarget() {
        AgentLifecycleState next =
            AgentLifecycleState.REGISTERED
                .transitionTo(AgentLifecycleState.STARTING);
        assertEquals(AgentLifecycleState.STARTING, next);
    }

    @Test
    void shouldThrowOnIllegalTransition() {
        assertThrows(
            IllegalStateException.class,
            () -> AgentLifecycleState.STOPPED
                .transitionTo(AgentLifecycleState.ACTIVE)
        );
    }

    @Test
    void shouldBeRunnableOnlyWhenActive() {
        for (AgentLifecycleState s : AgentLifecycleState.values()) {
            assertEquals(
                s == AgentLifecycleState.ACTIVE,
                s.isRunnable(),
                s + ".isRunnable() mismatch"
            );
        }
    }

    @Test
    void shouldBeTerminalForStoppedAndFailed() {
        assertTrue(AgentLifecycleState.STOPPED.isTerminal());
        assertTrue(AgentLifecycleState.FAILED.isTerminal());
        assertFalse(AgentLifecycleState.ACTIVE.isTerminal());
        assertFalse(AgentLifecycleState.DRAINING.isTerminal());
    }

    @Test
    void shouldBeDrainingOnlyWhenDraining() {
        assertTrue(AgentLifecycleState.DRAINING.isDraining());
        assertFalse(AgentLifecycleState.ACTIVE.isDraining());
    }

    @Test
    void shouldAllowRestartFromStopped() {
        // STOPPED → REGISTERED (for re-registration flow)
        assertTrue(
            AgentLifecycleState.STOPPED
                .canTransitionTo(AgentLifecycleState.REGISTERED)
        );
    }

    @Test
    void shouldAllowRestartFromFailed() {
        assertTrue(
            AgentLifecycleState.FAILED
                .canTransitionTo(AgentLifecycleState.REGISTERED)
        );
    }

    @Test
    void shouldAllowDrainingBackToActive() {
        // Recovery path: drain aborted, resume work
        assertTrue(
            AgentLifecycleState.DRAINING
                .canTransitionTo(AgentLifecycleState.ACTIVE)
        );
    }
}
