package io.github.regalpine.agentforge.cli;

public interface CliCommand {

    String name();

    CliOutput execute(
        String[] args
    );
}
