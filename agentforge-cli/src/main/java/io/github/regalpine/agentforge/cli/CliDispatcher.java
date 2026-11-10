package io.github.regalpine.agentforge.cli;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public final class CliDispatcher {

    private final Map<String, CliCommand> commands;

    public CliDispatcher(
        List<CliCommand> commandList
    ) {
        var map =
            new java.util.HashMap<String, CliCommand>();

        for (CliCommand cmd : commandList) {
            map.put(cmd.name(), cmd);
        }

        this.commands = Map.copyOf(map);
    }

    public CliOutput dispatch(
        String[] args
    ) {
        if (args == null || args.length == 0) {
            return CliOutput.failure(
                "Usage: agentforge <command> [args]"
            );
        }

        String commandName = args[0];

        CliCommand command =
            commands.get(commandName);

        if (command == null) {
            return CliOutput.failure(
                "Unknown command: " + commandName
            );
        }

        String[] subArgs =
            java.util.Arrays.copyOfRange(
                args,
                1,
                args.length
            );

        return command.execute(subArgs);
    }

    public Optional<CliCommand> find(
        String name
    ) {
        return Optional.ofNullable(
            commands.get(name)
        );
    }
}
