package io.github.regalpine.agentforge.cli;

import io.github.regalpine.agentforge.cli.command.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

/**
 * X0 CLI tests.
 * AF-ENHANCE-01 §6-8: All 8 commands.
 */
class X0CliTest {

    @Test
    void initCommandCreatesProject(
        @TempDir Path tempDir
    ) {
        InitCommand cmd = new InitCommand();
        assertEquals("init", cmd.name());

        Path projectDir =
            tempDir.resolve("test-project");

        CliOutput output = cmd.execute(
            new String[]{projectDir.toString()}
        );

        assertEquals(0, output.exitCode());
        assertTrue(
            Files.exists(
                projectDir.resolve("pom.xml")
            )
        );
        assertTrue(
            Files.exists(
                projectDir.resolve(
                    "agentforge.yaml"
                )
            )
        );
        assertTrue(
            Files.isDirectory(
                projectDir.resolve("agents")
            )
        );
        assertTrue(
            Files.isDirectory(
                projectDir.resolve("skills")
            )
        );
        assertTrue(
            Files.isDirectory(
                projectDir.resolve("src")
            )
        );
    }

    @Test
    void initCommandRejectsExisting(
        @TempDir Path tempDir
    ) {
        Path existing =
            tempDir.resolve("existing");
        try {
            Files.createDirectories(existing);
        } catch (IOException e) {
            fail(e);
        }

        InitCommand cmd = new InitCommand();
        CliOutput output = cmd.execute(
            new String[]{existing.toString()}
        );

        assertEquals(1, output.exitCode());
        assertTrue(
            output.output().contains(
                "already exists"
            )
        );
    }

    @Test
    void initCommandRequiresName() {
        InitCommand cmd = new InitCommand();
        CliOutput output =
            cmd.execute(new String[]{});

        assertEquals(1, output.exitCode());
        assertTrue(
            output.output().contains("Usage")
        );
    }

    @Test
    void devCommandRequiresProject(
        @TempDir Path tempDir
    ) {
        DevCommand cmd = new DevCommand();
        assertEquals("dev", cmd.name());

        CliOutput output = cmd.execute(
            new String[]{tempDir.toString()}
        );

        assertEquals(1, output.exitCode());
        assertTrue(
            output.output().contains(
                "Not an AgentForge"
            )
        );
    }

    @Test
    void devCommandAcceptsProject(
        @TempDir Path tempDir
    ) throws IOException {
        Files.writeString(
            tempDir.resolve("agentforge.yaml"),
            "project:\n  name: test\n"
        );

        DevCommand cmd = new DevCommand();
        CliOutput output = cmd.execute(
            new String[]{tempDir.toString()}
        );

        assertEquals(0, output.exitCode());
        assertTrue(
            output.output().contains("dev mode")
        );
    }

    @Test
    void runCommandRequiresAgentId() {
        RunCommand cmd = new RunCommand();
        assertEquals("run", cmd.name());

        CliOutput output =
            cmd.execute(new String[]{});

        assertEquals(1, output.exitCode());
        assertTrue(
            output.output().contains("Usage")
        );
    }

    @Test
    void testCommandRequiresTestDir(
        @TempDir Path tempDir
    ) {
        TestCommand cmd = new TestCommand();
        assertEquals("test", cmd.name());

        CliOutput output = cmd.execute(
            new String[]{tempDir.toString()}
        );

        assertEquals(1, output.exitCode());
    }

    @Test
    void debugCommandRequiresAgentId() {
        DebugCommand cmd = new DebugCommand();
        assertEquals("debug", cmd.name());

        CliOutput output =
            cmd.execute(new String[]{});

        assertEquals(1, output.exitCode());
        assertTrue(
            output.output().contains("Usage")
        );
    }

    @Test
    void validateCommandChecksProject(
        @TempDir Path tempDir
    ) throws IOException {
        Files.writeString(
            tempDir.resolve("agentforge.yaml"),
            "project:\n  name: test\n"
        );
        Files.createDirectory(
            tempDir.resolve("agents")
        );
        Files.writeString(
            tempDir.resolve("agents/test.yaml"),
            "id: test\nmodel:\n  ref: default\n"
        );

        ValidateCommand cmd =
            new ValidateCommand();
        assertEquals("validate", cmd.name());

        CliOutput output = cmd.execute(
            new String[]{tempDir.toString()}
        );

        assertEquals(0, output.exitCode());
        assertTrue(
            output.output().contains("passed")
        );
    }

    @Test
    void validateCommandDetectsMissing(
        @TempDir Path tempDir
    ) {
        ValidateCommand cmd =
            new ValidateCommand();

        CliOutput output = cmd.execute(
            new String[]{tempDir.toString()}
        );

        assertEquals(1, output.exitCode());
        assertTrue(
            output.output().contains("ERROR")
        );
    }

    @Test
    void buildCommandRequiresProject(
        @TempDir Path tempDir
    ) {
        BuildCommand cmd = new BuildCommand();
        assertEquals("build", cmd.name());

        CliOutput output = cmd.execute(
            new String[]{tempDir.toString()}
        );

        assertEquals(1, output.exitCode());
    }

    @Test
    void buildCommandRequiresPom(
        @TempDir Path tempDir
    ) throws IOException {
        Files.writeString(
            tempDir.resolve("agentforge.yaml"),
            "project:\n  name: test\n"
        );

        BuildCommand cmd = new BuildCommand();
        CliOutput output = cmd.execute(
            new String[]{tempDir.toString()}
        );

        assertEquals(1, output.exitCode());
        assertTrue(
            output.output().contains("pom.xml")
        );
    }

    @Test
    void cliDispatcherRegistersAll() {
        var commands = java.util.List.of(
            new InitCommand(),
            new DevCommand(),
            new RunCommand(),
            new TestCommand(),
            new DebugCommand(),
            new InspectCommand(),
            new ValidateCommand(),
            new BuildCommand()
        );

        CliDispatcher dispatcher =
            new CliDispatcher(commands);

        assertEquals(8,
            commands.size());

        for (var cmd : commands) {
            assertTrue(
                dispatcher.find(cmd.name())
                    .isPresent(),
                "Command not found: "
                    + cmd.name()
            );
        }
    }
}
