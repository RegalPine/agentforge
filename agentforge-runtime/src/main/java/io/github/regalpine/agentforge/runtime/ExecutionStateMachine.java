package io.github.regalpine.agentforge.runtime;

import io.github.regalpine.agentforge.event.ExecutionStatus;
public final class ExecutionStateMachine {

    public void verify(
        ExecutionStatus from,
        ExecutionStatus to
    ) {

        boolean valid =
            switch (from) {

                case CREATED ->
                    to == ExecutionStatus.READY;

                case READY ->
                    to == ExecutionStatus.RUNNING ||
                    to == ExecutionStatus.SUSPENDED ||
                    to == ExecutionStatus.CANCELLED;

                case RUNNING ->
                    to == ExecutionStatus.WAITING ||
                    to == ExecutionStatus.COMPLETED ||
                    to == ExecutionStatus.FAILED ||
                    to == ExecutionStatus.CANCELLED ||
                    to == ExecutionStatus.SUSPENDED;

                case WAITING ->
                    to == ExecutionStatus.RUNNING ||
                    to == ExecutionStatus.CANCELLED;

                case SUSPENDED ->
                    to == ExecutionStatus.READY ||
                    to == ExecutionStatus.CANCELLED;

                case COMPLETED,
                     FAILED,
                     CANCELLED ->
                    false;
            };

        if (!valid) {
            throw new IllegalStateException(
                "Invalid execution state transition: "
                    + from + " -> " + to
            );
        }
    }
}
