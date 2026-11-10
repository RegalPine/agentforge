package io.github.regalpine.agentforge.cli.command;

import io.github.regalpine.agentforge.cli.CliCommand;
import io.github.regalpine.agentforge.cli.CliOutput;

import java.nio.file.Files;
import java.nio.file.Path;

/**
 * CLI debug command.
 * AF-ENHANCE-01 §7: agentforge debug.
 *
 * <p>Starts a debug session for
 * an agent execution.
 */
public final class DebugCommand
    implements CliCommand {

    @Override
    public String name() {
        return "debug";
    }

    @Override
    public CliOutput execute(
        String[] args
    ) {
        if (args.length < 1
            || args[0].isBlank()
        ) {
            return CliOutput.failure(
                "Usage: agentforge debug "
                    + "<agent-id> [options]"
            );
        }

        String agentId = args[0];

        Path projectDir = Path.of(".");

        if (!Files.exists(
            projectDir.resolve("agentforge.yaml")
        )) {
            return CliOutput.failure(
                "Not an AgentForge project.\n"
                    + "Run from project root."
            );
        }

        return CliOutput.success(
            "Starting debug session for agent '"
                + agentId + "'...\n"
                + "  Debug mode: enabled\n"
                + "  Breakpoints: available\n"
                + "  (Debugger session "
                + "placeholder — "
                + "full Debugger in X3)"
        );
    }
}
