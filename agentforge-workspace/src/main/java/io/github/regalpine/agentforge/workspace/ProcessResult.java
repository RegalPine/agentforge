package io.github.regalpine.agentforge.workspace;

import java.time.Duration;
import java.time.Instant;

/**
 * Process execution result.
 * AF-ENHANCE-07 §32/49: Process
 * Execution Result.
 *
 * <p>Records: exit code, stdout,
 * stderr, duration, resource usage.
 */
public record ProcessResult(
    String processId,
    int exitCode,
    String stdout,
    String stderr,
    Duration duration,
    Instant startedAt,
    Instant completedAt,
    boolean timedOut,
    boolean killed
) {

    public ProcessResult {
        processId =
            processId == null
                ? "" : processId;
        stdout =
            stdout == null
                ? "" : stdout;
        stderr =
            stderr == null
                ? "" : stderr;
        duration =
            duration == null
                ? Duration.ZERO
                : duration;
        startedAt =
            startedAt == null
                ? Instant.now()
                : startedAt;
        completedAt =
            completedAt == null
                ? Instant.now()
                : completedAt;
    }

    /**
     * Whether the process succeeded.
     */
    public boolean isSuccess() {
        return exitCode == 0
            && !timedOut
            && !killed;
    }

    /**
     * Create a success result.
     */
    public static ProcessResult success(
        String processId,
        String stdout,
        Duration duration
    ) {
        return new ProcessResult(
            processId, 0, stdout, "",
            duration, Instant.now(),
            Instant.now().plus(duration),
            false, false
        );
    }

    /**
     * Create a failure result.
     */
    public static ProcessResult failure(
        String processId,
        int exitCode,
        String stderr,
        Duration duration
    ) {
        return new ProcessResult(
            processId, exitCode, "",
            stderr, duration,
            Instant.now(),
            Instant.now().plus(duration),
            false, false
        );
    }

    /**
     * Create a timeout result.
     */
    public static ProcessResult timeout(
        String processId,
        Duration duration
    ) {
        return new ProcessResult(
            processId, -1, "",
            "Execution timed out",
            duration, Instant.now(),
            Instant.now().plus(duration),
            true, true
        );
    }
}
