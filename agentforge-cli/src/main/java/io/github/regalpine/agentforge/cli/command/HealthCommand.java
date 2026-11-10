package io.github.regalpine.agentforge.cli.command;

import io.github.regalpine.agentforge.cli.CliCommand;
import io.github.regalpine.agentforge.cli.CliOutput;

public final class HealthCommand
    implements CliCommand {

    @Override
    public String name() {
        return "health";
    }

    @Override
    public CliOutput execute(
        String[] args
    ) {
        return CliOutput.success(
            "AgentForge server is healthy"
        );
    }
}
