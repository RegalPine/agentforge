package io.github.regalpine.agentforge.cli;

import io.github.regalpine.agentforge.cli.command.SkillCommands;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * CLI Skill 命令族测试.
 * T-S5.25: 12 命令 / 退出码 / 不绕过安全.
 */
class SkillCommandsTest {

    private final SkillCommands cmd =
        new SkillCommands();

    // ── 基本路由 ──────────────

    @Test
    void shouldHaveNameSkill() {
        assertEquals("skill", cmd.name());
    }

    @Test
    void shouldHave12Subcommands() {
        assertEquals(
            12, cmd.subcommands().size()
        );
    }

    @Test
    void shouldFailWithNoArgs() {
        CliOutput out = cmd.execute(new String[0]);
        assertEquals(1, out.exitCode());
        assertTrue(out.output().contains("Usage"));
    }

    @Test
    void shouldFailWithUnknownSubcommand() {
        CliOutput out = cmd.execute(
            new String[]{"unknown"}
        );
        assertEquals(1, out.exitCode());
        assertTrue(
            out.output().contains("Unknown")
        );
    }

    // ── list ──────────────────

    @Test
    void shouldHandleList() {
        CliOutput out = cmd.execute(
            new String[]{"list"}
        );
        assertEquals(0, out.exitCode());
        assertTrue(
            out.output().contains("SKILL_LIST")
        );
    }

    // ── inspect ───────────────

    @Test
    void shouldHandleInspect() {
        CliOutput out = cmd.execute(
            new String[]{"inspect", "s1", "1.0.0"}
        );
        assertEquals(0, out.exitCode());
        assertTrue(
            out.output().contains("s1")
        );
        assertTrue(
            out.output().contains("1.0.0")
        );
    }

    @Test
    void inspectShouldRequireSkillId() {
        CliOutput out = cmd.execute(
            new String[]{"inspect"}
        );
        assertEquals(1, out.exitCode());
        assertTrue(
            out.output().contains("Usage")
        );
    }

    // ── validate ──────────────

    @Test
    void shouldHandleValidate() {
        CliOutput out = cmd.execute(
            new String[]{"validate", "s1"}
        );
        assertEquals(0, out.exitCode());
        assertTrue(
            out.output().contains("SKILL_VALIDATE")
        );
    }

    // ── test ──────────────────

    @Test
    void shouldHandleTest() {
        CliOutput out = cmd.execute(
            new String[]{"test", "s1"}
        );
        assertEquals(0, out.exitCode());
        assertTrue(
            out.output().contains("SKILL_TEST")
        );
    }

    // ── evaluate ──────────────

    @Test
    void shouldHandleEvaluate() {
        CliOutput out = cmd.execute(
            new String[]{"evaluate", "s1"}
        );
        assertEquals(0, out.exitCode());
        assertTrue(
            out.output().contains("SKILL_EVALUATE")
        );
    }

    // ── package ───────────────

    @Test
    void shouldHandlePackage() {
        CliOutput out = cmd.execute(
            new String[]{"package", "/tmp/skill"}
        );
        assertEquals(0, out.exitCode());
        assertTrue(
            out.output().contains("SKILL_PACKAGE")
        );
    }

    @Test
    void packageShouldDefaultToCurrentDir() {
        CliOutput out = cmd.execute(
            new String[]{"package"}
        );
        assertEquals(0, out.exitCode());
        assertTrue(out.output().contains("."));
    }

    // ── publish ───────────────

    @Test
    void shouldHandlePublish() {
        CliOutput out = cmd.execute(
            new String[]{"publish", "s1", "1.0.0"}
        );
        assertEquals(0, out.exitCode());
        assertTrue(
            out.output().contains("SKILL_PUBLISH")
        );
    }

    @Test
    void publishShouldRequireArgs() {
        CliOutput out = cmd.execute(
            new String[]{"publish", "s1"}
        );
        assertEquals(1, out.exitCode());
    }

    // ── activate ──────────────

    @Test
    void shouldHandleActivate() {
        CliOutput out = cmd.execute(
            new String[]{"activate", "s1", "1.0.0"}
        );
        assertEquals(0, out.exitCode());
        assertTrue(
            out.output().contains("ACTIVE")
        );
    }

    // ── deprecate ─────────────

    @Test
    void shouldHandleDeprecate() {
        CliOutput out = cmd.execute(
            new String[]{"deprecate", "s1", "1.0.0"}
        );
        assertEquals(0, out.exitCode());
        assertTrue(
            out.output().contains("DEPRECATED")
        );
    }

    // ── retire ────────────────

    @Test
    void shouldHandleRetire() {
        CliOutput out = cmd.execute(
            new String[]{"retire", "s1", "1.0.0"}
        );
        assertEquals(0, out.exitCode());
        assertTrue(
            out.output().contains("RETIRED")
        );
    }

    // ── revoke ────────────────

    @Test
    void shouldHandleRevoke() {
        CliOutput out = cmd.execute(
            new String[]{"revoke", "s1", "1.0.0"}
        );
        assertEquals(0, out.exitCode());
        assertTrue(
            out.output().contains("REVOKED")
        );
    }

    // ── invoke ────────────────

    @Test
    void shouldHandleInvoke() {
        CliOutput out = cmd.execute(
            new String[]{
                "invoke", "s1", "1.0.0",
                "{\"key\":\"value\"}"
            }
        );
        assertEquals(0, out.exitCode());
        assertTrue(
            out.output().contains("SKILL_INVOKE")
        );
        assertTrue(
            out.output().contains("security")
        );
    }

    @Test
    void invokeShouldRequireSkillId() {
        CliOutput out = cmd.execute(
            new String[]{"invoke"}
        );
        assertEquals(1, out.exitCode());
    }

    // ── Exit codes ────────────

    @Test
    void exitCodesShouldBeCorrect() {
        assertEquals(0, SkillExitCodes.SUCCESS);
        assertEquals(1, SkillExitCodes.GENERAL_ERROR);
        assertEquals(2, SkillExitCodes.USAGE_ERROR);
        assertEquals(3, SkillExitCodes.SKILL_NOT_FOUND);
        assertEquals(4, SkillExitCodes.SECURITY_DENIED);
        assertEquals(5, SkillExitCodes.LIFECYCLE_ERROR);
        assertEquals(10, SkillExitCodes.NETWORK_ERROR);
    }

    @Test
    void shouldCreateOutputWithCode() {
        var out = SkillExitCodes.withCode(
            SkillExitCodes.SKILL_NOT_FOUND,
            "Not found"
        );
        assertEquals(3, out.exitCode());
        assertEquals("Not found", out.output());
    }

    // ── Security ──────────────

    @Test
    void cliShouldNotExecuteRuntimeLogic() {
        // CLI delegates, never executes directly
        CliOutput out = cmd.execute(
            new String[]{"invoke", "s1"}
        );
        assertTrue(
            out.output().contains("Delegating")
                || out.output().contains("security")
        );
    }
}
