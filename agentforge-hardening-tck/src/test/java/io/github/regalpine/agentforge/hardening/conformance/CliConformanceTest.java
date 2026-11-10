package io.github.regalpine.agentforge.hardening.conformance;

import io.github.regalpine.agentforge.cli.CliDispatcher;
import io.github.regalpine.agentforge.cli.command.*;
import io.github.regalpine.agentforge.hardening.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * conformance-cli: Create / Validate
 * / Run / Doctor.
 * AF-RELEASE-02 §4, T-H6.14.
 */
class CliConformanceTest {

    @Test
    void afCli001_createCommandExists() {
        var cmd = new CreateCommand();
        assertEquals("create", cmd.name());
    }

    @Test
    void afCli002_doctorCommandExists() {
        var cmd = new DoctorCommand();
        assertEquals("doctor", cmd.name());
    }

    @Test
    void afCli003_initCommandExists() {
        var cmd = new InitCommand();
        assertEquals("init", cmd.name());
    }

    @Test
    void afCli004_createTemplates() {
        var cmd = new CreateCommand();
        var output = cmd.execute(new String[0]);
        assertTrue(
            output.output().contains("basic-agent")
        );
    }

    @Test
    void afCli005_doctorRuns(@TempDir Path dir) {
        var cmd = new DoctorCommand();
        var output = cmd.execute(new String[]{"--verbose"});
        assertNotNull(output);
        assertTrue(output.output().contains("Java"));
    }

    @Test
    void afCli006_dispatcherRegisters() {
        var dispatcher = new CliDispatcher(
            List.of(
                new CreateCommand(),
                new DoctorCommand(),
                new InitCommand()
            )
        );
        assertNotNull(dispatcher);
    }
}
