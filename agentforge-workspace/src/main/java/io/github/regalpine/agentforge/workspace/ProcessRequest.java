package io.github.regalpine.agentforge.workspace;

import java.util.List;
import java.util.Map;

/**
 * Process request record.
 * AF-ENHANCE-07 §33: Process Request.
 *
 * <p>Executable + Arguments (not shell
 * string) to prevent command injection.
 */
public record ProcessRequest(
    String executable,
    List<String> arguments,
    Map<String, String> environment,
    String workingDir
) {

    public ProcessRequest {
        if (executable == null
            || executable.isBlank()
        ) {
            throw new IllegalArgumentException(
                "executable required"
            );
        }

        arguments =
            arguments == null
                ? List.of()
                : List.copyOf(arguments);

        environment =
            environment == null
                ? Map.of()
                : Map.copyOf(environment);

        workingDir =
            workingDir == null
                ? "" : workingDir;
    }

    /**
     * Create a simple request.
     */
    public static ProcessRequest of(
        String executable,
        String... args
    ) {
        return new ProcessRequest(
            executable,
            List.of(args),
            null, null
        );
    }
}
