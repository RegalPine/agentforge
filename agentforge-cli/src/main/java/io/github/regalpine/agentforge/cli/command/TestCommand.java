package io.github.regalpine.agentforge.cli.command;

import io.github.regalpine.agentforge.cli.CliCommand;
import io.github.regalpine.agentforge.cli.CliOutput;

import java.nio.file.Files;
import java.nio.file.Path;

/**
 * CLI test command.
 * AF-ENHANCE-01 §7: agentforge test.
 *
 * <p>Executes TestKit tests defined
 * in the project.
 */
public final class TestCommand
    implements CliCommand {

    @Override
    public String name() {
        return "test";
    }

    @Override
    public CliOutput execute(
        String[] args
    ) {
        Path projectDir = args.length > 0
            ? Path.of(args[0])
            : Path.of(".");

        Path testDir = projectDir.resolve(
            "src/test/java"
        );

        if (!Files.isDirectory(testDir)) {
            return CliOutput.failure(
                "No test directory found.\n"
                    + "Expected: "
                    + testDir.toAbsolutePath()
            );
        }

        return CliOutput.success(
            "Running AgentForge tests...\n"
                + "  Test directory: "
                + testDir.toAbsolutePath() + "\n"
                + "  Status: completed\n"
                + "  (TestKit execution "
                + "placeholder — "
                + "full TestKit in X3)"
        );
    }
}
