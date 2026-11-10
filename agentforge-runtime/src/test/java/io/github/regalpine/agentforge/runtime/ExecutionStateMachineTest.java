package io.github.regalpine.agentforge.runtime;

import io.github.regalpine.agentforge.event.ExecutionStatus;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ExecutionStateMachineTest {

    private final ExecutionStateMachine machine =
        new ExecutionStateMachine();

    @Test
    void shouldAllowCreatedToReady() {
        machine.verify(
            ExecutionStatus.CREATED,
            ExecutionStatus.READY
        );
    }

    @Test
    void shouldAllowReadyToRunning() {
        machine.verify(
            ExecutionStatus.READY,
            ExecutionStatus.RUNNING
        );
    }

    @Test
    void shouldAllowRunningToWaiting() {
        machine.verify(
            ExecutionStatus.RUNNING,
            ExecutionStatus.WAITING
        );
    }

    @Test
    void shouldAllowRunningToCompleted() {
        machine.verify(
            ExecutionStatus.RUNNING,
            ExecutionStatus.COMPLETED
        );
    }

    @Test
    void shouldAllowRunningToFailed() {
        machine.verify(
            ExecutionStatus.RUNNING,
            ExecutionStatus.FAILED
        );
    }

    @Test
    void shouldAllowRunningToCancelled() {
        machine.verify(
            ExecutionStatus.RUNNING,
            ExecutionStatus.CANCELLED
        );
    }

    @Test
    void shouldAllowRunningToSuspended() {
        machine.verify(
            ExecutionStatus.RUNNING,
            ExecutionStatus.SUSPENDED
        );
    }

    @Test
    void shouldAllowWaitingToRunning() {
        machine.verify(
            ExecutionStatus.WAITING,
            ExecutionStatus.RUNNING
        );
    }

    @Test
    void shouldAllowReadyToSuspended() {
        machine.verify(
            ExecutionStatus.READY,
            ExecutionStatus.SUSPENDED
        );
    }

    @Test
    void shouldAllowSuspendedToReady() {
        machine.verify(
            ExecutionStatus.SUSPENDED,
            ExecutionStatus.READY
        );
    }

    @Test
    void shouldRejectCompletedToAnything() {
        for (ExecutionStatus to : ExecutionStatus.values()) {
            assertThrows(
                IllegalStateException.class,
                () -> machine.verify(
                    ExecutionStatus.COMPLETED, to
                )
            );
        }
    }

    @Test
    void shouldRejectFailedToAnything() {
        for (ExecutionStatus to : ExecutionStatus.values()) {
            assertThrows(
                IllegalStateException.class,
                () -> machine.verify(
                    ExecutionStatus.FAILED, to
                )
            );
        }
    }

    @Test
    void shouldRejectCancelledToAnything() {
        for (ExecutionStatus to : ExecutionStatus.values()) {
            assertThrows(
                IllegalStateException.class,
                () -> machine.verify(
                    ExecutionStatus.CANCELLED, to
                )
            );
        }
    }

    @Test
    void shouldRejectCreatedToRunning() {
        assertThrows(
            IllegalStateException.class,
            () -> machine.verify(
                ExecutionStatus.CREATED,
                ExecutionStatus.RUNNING
            )
        );
    }
}
