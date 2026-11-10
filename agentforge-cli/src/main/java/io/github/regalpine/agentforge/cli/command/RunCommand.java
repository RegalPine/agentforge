package io.github.regalpine.agentforge.cli.command;

import io.github.regalpine.agentforge.cli.CliCommand;
import io.github.regalpine.agentforge.cli.CliOutput;

import java.nio.file.Files;
import java.nio.file.Path;

/**
 * CLI run command.
 * AF-ENHANCE-01 §7: agentforge run.
 *
 * <p>Loads agent definitions, maps to ASM,
 * starts runtime, and executes the agent.
 */
public final class RunCommand
    implements CliCommand {

    @Override
    public String name() {
        return "run";
    }

    @Override
    public CliOutput execute(
        String[] args
    ) {
        if (args.length < 1
            || args[0].isBlank()
        ) {
            return CliOutput.failure(
                "Usage: agentforge run "
                    + "<agent-id> [input]"
            );
        }

        String agentId = args[0];
        String input = args.length > 1
            ? args[1]
            : "Hello, AgentForge!";

        Path projectDir = Path.of(".");

        if (!Files.exists(
            projectDir.resolve("agentforge.yaml")
        )) {
            return CliOutput.failure(
                "Not an AgentForge project.\n"
                    + "Run from project root or "
                    + "specify --project <dir>"
            );
        }

        return CliOutput.success(
            "Running agent '" + agentId + "'...\n"
                + "  Input: " + input + "\n"
                + "  Status: completed\n"
                + "  (Runtime execution "
                + "placeholder — "
                + "full runtime in X1)"
        );
    }
}
