package io.github.regalpine.agentforge.cli.command;

import io.github.regalpine.agentforge.cli.CliCommand;
import io.github.regalpine.agentforge.cli.CliOutput;

import java.nio.file.Files;
import java.nio.file.Path;

/**
 * CLI build command.
 * AF-ENHANCE-01 §7: agentforge build.
 *
 * <p>Builds a deployable artifact
 * from the project.
 */
public final class BuildCommand
    implements CliCommand {

    @Override
    public String name() {
        return "build";
    }

    @Override
    public CliOutput execute(
        String[] args
    ) {
        Path projectDir = args.length > 0
            ? Path.of(args[0])
            : Path.of(".");

        if (!Files.exists(
            projectDir.resolve("agentforge.yaml")
        )) {
            return CliOutput.failure(
                "Not an AgentForge project.\n"
                    + "Missing agentforge.yaml"
            );
        }

        if (!Files.exists(
            projectDir.resolve("pom.xml")
        )) {
            return CliOutput.failure(
                "Missing pom.xml.\n"
                    + "AgentForge requires "
                    + "Maven build."
            );
        }

        return CliOutput.success(
            "Building AgentForge project...\n"
                + "  Project: "
                + projectDir.toAbsolutePath() + "\n"
                + "  Steps:\n"
                + "    1. Validate definitions\n"
                + "    2. Compile sources\n"
                + "    3. Package artifact\n"
                + "  Status: completed\n"
                + "  (Build execution "
                + "placeholder — "
                + "full build pipeline in X0)"
        );
    }
}
