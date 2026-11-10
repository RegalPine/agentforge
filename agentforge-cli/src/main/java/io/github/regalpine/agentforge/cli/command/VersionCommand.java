package io.github.regalpine.agentforge.cli.command;

import io.github.regalpine.agentforge.cli.CliCommand;
import io.github.regalpine.agentforge.cli.CliOutput;

public final class VersionCommand
    implements CliCommand {

    private final String version;

    public VersionCommand() {
        this("1.0.0-SNAPSHOT");
    }

    public VersionCommand(
        String version
    ) {
        this.version = version;
    }

    @Override
    public String name() {
        return "version";
    }

    @Override
    public CliOutput execute(
        String[] args
    ) {
        return CliOutput.success(
            "agentforge " + version
        );
    }
}
