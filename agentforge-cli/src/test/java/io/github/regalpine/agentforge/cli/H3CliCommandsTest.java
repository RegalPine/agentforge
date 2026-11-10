package io.github.regalpine.agentforge.cli;

import io.github.regalpine.agentforge.cli.command.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for H3 CLI commands:
 * CreateCommand, DoctorCommand.
 */
class H3CliCommandsTest {

    // --- CreateCommand ---

    @Test
    void createShouldListTemplates() {
        var cmd = new CreateCommand();

        CliOutput output = cmd.execute(new String[0]);

        assertEquals(1, output.exitCode());
        assertTrue(
            output.output().contains("basic-agent")
        );
        assertTrue(
            output.output().contains("graph-agent")
        );
        assertTrue(
            output.output().contains("rag-agent")
        );
        assertTrue(
            output.output().contains("tool-agent")
        );
        assertTrue(
            output.output().contains("multi-agent")
        );
    }

    @Test
    void createShouldRejectUnknownTemplate() {
        var cmd = new CreateCommand();

        CliOutput output = cmd.execute(
            new String[]{"nonexistent"}
        );

        assertEquals(1, output.exitCode());
        assertTrue(
            output.output().contains("Unknown")
        );
    }

    @Test
    void createShouldGenerateBasicAgent(
        @TempDir Path tempDir
    ) {
        var cmd = new CreateCommand();
        Path projectDir =
            tempDir.resolve("test-basic");

        CliOutput output = cmd.execute(
            new String[]{
                "basic-agent",
                projectDir.toString()
            }
        );

        assertEquals(0, output.exitCode());
        assertTrue(output.output()
            .contains("Created project"));
        assertTrue(Files.exists(
            projectDir.resolve("agentforge.yaml")
        ));
    }

    @Test
    void createShouldGenerateGraphAgent(
        @TempDir Path tempDir
    ) {
        var cmd = new CreateCommand();
        Path projectDir =
            tempDir.resolve("test-graph");

        CliOutput output = cmd.execute(
            new String[]{
                "graph-agent",
                projectDir.toString()
            }
        );

        assertEquals(0, output.exitCode());
        assertTrue(Files.exists(
            projectDir.resolve("agentforge.yaml")
        ));
    }

    @Test
    void createShouldGenerateRagAgent(
        @TempDir Path tempDir
    ) {
        var cmd = new CreateCommand();
        Path projectDir =
            tempDir.resolve("test-rag");

        CliOutput output = cmd.execute(
            new String[]{
                "rag-agent",
                projectDir.toString()
            }
        );

        assertEquals(0, output.exitCode());
        assertTrue(Files.exists(
            projectDir.resolve("agentforge.yaml")
        ));
    }

    @Test
    void createShouldGenerateToolAgent(
        @TempDir Path tempDir
    ) {
        var cmd = new CreateCommand();
        Path projectDir =
            tempDir.resolve("test-tool");

        CliOutput output = cmd.execute(
            new String[]{
                "tool-agent",
                projectDir.toString()
            }
        );

        assertEquals(0, output.exitCode());
        assertTrue(Files.exists(
            projectDir.resolve("agentforge.yaml")
        ));
    }

    @Test
    void createShouldGenerateMultiAgent(
        @TempDir Path tempDir
    ) {
        var cmd = new CreateCommand();
        Path projectDir =
            tempDir.resolve("test-multi");

        CliOutput output = cmd.execute(
            new String[]{
                "multi-agent",
                projectDir.toString()
            }
        );

        assertEquals(0, output.exitCode());
        assertTrue(Files.exists(
            projectDir.resolve("agentforge.yaml")
        ));
    }

    @Test
    void createTemplateNamesShouldBeFive() {
        assertEquals(5,
            CreateCommand.templateNames().size());
    }

    // --- DoctorCommand ---

    @Test
    void doctorShouldRunChecks() {
        var cmd = new DoctorCommand();

        CliOutput output = cmd.execute(new String[0]);

        // Doctor always runs, may have warnings
        assertNotNull(output.output());
        assertTrue(
            output.output().contains("Doctor Report")
        );
        assertTrue(
            output.output().contains("Java Version")
        );
        assertTrue(
            output.output().contains("Summary")
        );
    }

    @Test
    void doctorShouldSupportVerbose() {
        var cmd = new DoctorCommand();

        CliOutput output = cmd.execute(
            new String[]{"--verbose"}
        );

        assertNotNull(output.output());
        assertTrue(
            output.output().contains("Doctor Report")
        );
    }

    @Test
    void doctorShouldCheckJavaVersion() {
        var cmd = new DoctorCommand();

        CliOutput output = cmd.execute(new String[0]);

        // Java 17+ should pass
        assertTrue(
            output.output().contains("[OK]")
        );
    }

    // --- Dispatcher integration ---

    @Test
    void dispatcherShouldIncludeNewCommands() {
        var dispatcher = new CliDispatcher(
            List.of(
                new VersionCommand(),
                new InitCommand(),
                new HealthCommand(),
                new CreateCommand(),
                new DoctorCommand()
            )
        );

        assertTrue(
            dispatcher.find("create").isPresent()
        );
        assertTrue(
            dispatcher.find("doctor").isPresent()
        );
    }
}
