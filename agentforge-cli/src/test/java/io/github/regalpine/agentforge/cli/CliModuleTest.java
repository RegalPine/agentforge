package io.github.regalpine.agentforge.cli;

import io.github.regalpine.agentforge.cli.command.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class CliModuleTest {

    @Test
    void shouldReturnVersion() {
        var cmd = new VersionCommand();

        CliOutput output = cmd.execute(new String[0]);

        assertEquals(0, output.exitCode());
        assertTrue(
            output.output().contains("agentforge")
        );
    }

    @Test
    void shouldReturnCustomVersion() {
        var cmd = new VersionCommand("2.0.0");

        CliOutput output = cmd.execute(new String[0]);

        assertEquals(0, output.exitCode());
        assertTrue(
            output.output().contains("2.0.0")
        );
    }

    @Test
    void shouldInitProject(@TempDir Path tempDir) {
        var cmd = new InitCommand();

        String projectPath =
            tempDir.resolve("my-project").toString();

        CliOutput output =
            cmd.execute(new String[]{projectPath});

        assertEquals(0, output.exitCode());
        assertTrue(
            output.output().contains("my-project")
        );
    }

    @Test
    void shouldInitDefaultDir() {
        var cmd = new InitCommand();

        // AF-ENHANCE-01: init requires project name
        CliOutput output = cmd.execute(new String[0]);

        assertEquals(1, output.exitCode());
        assertTrue(
            output.output().contains("Usage")
        );
    }

    @Test
    void shouldReportHealth() {
        var cmd = new HealthCommand();

        CliOutput output = cmd.execute(new String[0]);

        assertEquals(0, output.exitCode());
        assertTrue(
            output.output().contains("healthy")
        );
    }

    @Test
    void shouldDispatchCommand() {
        var dispatcher =
            new CliDispatcher(
                List.of(
                    new VersionCommand(),
                    new InitCommand(),
                    new HealthCommand()
                )
            );

        CliOutput output =
            dispatcher.dispatch(
                new String[]{"version"}
            );

        assertEquals(0, output.exitCode());
        assertTrue(
            output.output().contains("agentforge")
        );
    }

    @Test
    void shouldRejectUnknownCommand() {
        var dispatcher =
            new CliDispatcher(
                List.of(new VersionCommand())
            );

        CliOutput output =
            dispatcher.dispatch(
                new String[]{"unknown-cmd"}
            );

        assertEquals(1, output.exitCode());
        assertTrue(
            output.output().contains("Unknown")
        );
    }

    @Test
    void shouldRejectEmptyArgs() {
        var dispatcher =
            new CliDispatcher(List.of());

        CliOutput output =
            dispatcher.dispatch(new String[0]);

        assertEquals(1, output.exitCode());
        assertTrue(
            output.output().contains("Usage")
        );
    }

    @Test
    void shouldFindCommand() {
        var dispatcher =
            new CliDispatcher(
                List.of(new VersionCommand())
            );

        assertTrue(
            dispatcher.find("version").isPresent()
        );

        assertFalse(
            dispatcher.find("missing").isPresent()
        );
    }
}
