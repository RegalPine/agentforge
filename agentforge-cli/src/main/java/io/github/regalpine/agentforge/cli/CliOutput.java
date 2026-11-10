package io.github.regalpine.agentforge.cli;

public record CliOutput(
    int exitCode,
    String output
) {

    public static CliOutput success(
        String output
    ) {
        return new CliOutput(0, output);
    }

    public static CliOutput failure(
        String output
    ) {
        return new CliOutput(1, output);
    }
}
