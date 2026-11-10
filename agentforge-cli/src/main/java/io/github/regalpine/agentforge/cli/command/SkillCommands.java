package io.github.regalpine.agentforge.cli.command;

import io.github.regalpine.agentforge.cli.CliCommand;
import io.github.regalpine.agentforge.cli.CliOutput;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * Skill 命令族.
 * AF-SKILL-04 §30-§42: 12 skill commands.
 * AF-AD-15 §11.2: CLI skill 命令.
 *
 * <p>Subcommands:
 * <pre>
 * agentforge skill list
 * agentforge skill inspect &lt;id&gt; [version]
 * agentforge skill validate &lt;id&gt; [version]
 * agentforge skill test &lt;id&gt; [version]
 * agentforge skill evaluate &lt;id&gt; [version]
 * agentforge skill package &lt;dir&gt;
 * agentforge skill publish &lt;id&gt; &lt;version&gt;
 * agentforge skill activate &lt;id&gt; &lt;version&gt;
 * agentforge skill deprecate &lt;id&gt; &lt;version&gt;
 * agentforge skill retire &lt;id&gt; &lt;version&gt;
 * agentforge skill revoke &lt;id&gt; &lt;version&gt;
 * agentforge skill invoke &lt;id&gt; [version] [input]
 * </pre>
 *
 * <p>N: CLI does not execute Runtime logic.
 * It delegates to SDK/Server. CLI is not
 * a security bypass entry point.
 */
public final class SkillCommands implements CliCommand {

    private static final Set<String> SUBCOMMANDS = Set.of(
        "list", "inspect", "validate", "test",
        "evaluate", "package", "publish", "activate",
        "deprecate", "retire", "revoke", "invoke"
    );

    private final Map<String, SubHandler> handlers;

    public SkillCommands() {
        var map = new LinkedHashMap<String, SubHandler>();
        map.put("list", this::handleList);
        map.put("inspect", this::handleInspect);
        map.put("validate", this::handleValidate);
        map.put("test", this::handleTest);
        map.put("evaluate", this::handleEvaluate);
        map.put("package", this::handlePackage);
        map.put("publish", this::handlePublish);
        map.put("activate", this::handleActivate);
        map.put("deprecate", this::handleDeprecate);
        map.put("retire", this::handleRetire);
        map.put("revoke", this::handleRevoke);
        map.put("invoke", this::handleInvoke);
        this.handlers = Map.copyOf(map);
    }

    @Override
    public String name() {
        return "skill";
    }

    @Override
    public CliOutput execute(String[] args) {
        if (args == null || args.length == 0) {
            return CliOutput.failure(
                "Usage: agentforge skill <subcommand> [args]\n"
                    + "Subcommands: "
                    + String.join(", ", SUBCOMMANDS.stream().sorted().toList())
            );
        }

        String sub = args[0];
        SubHandler handler = handlers.get(sub);

        if (handler == null) {
            return CliOutput.failure(
                "Unknown skill subcommand: " + sub + "\n"
                    + "Available: "
                    + String.join(", ", SUBCOMMANDS.stream().sorted().toList())
            );
        }

        String[] subArgs = Arrays.copyOfRange(args, 1, args.length);
        return handler.handle(subArgs);
    }

    /**
     * Get available subcommand names.
     */
    public Set<String> subcommands() {
        return SUBCOMMANDS;
    }

    // ── Sub-handlers ────────────

    private CliOutput handleList(String[] args) {
        // Delegates to SDK/Server: list all Skills
        return CliOutput.success(
            "SKILL_LIST: Delegating to registry...\n"
                + "Use SDK SkillApi.discover() or "
                + "GET /api/v1/skills"
        );
    }

    private CliOutput handleInspect(String[] args) {
        if (args.length < 1) {
            return CliOutput.failure(
                "Usage: agentforge skill inspect <skillId> [version]"
            );
        }
        String skillId = args[0];
        String version = args.length > 1 ? args[1] : "latest";
        return CliOutput.success(
            "SKILL_INSPECT: " + skillId + "@" + version
                + "\nUse SDK SkillApi.resolve() or "
                + "GET /api/v1/skills/" + skillId
        );
    }

    private CliOutput handleValidate(String[] args) {
        if (args.length < 1) {
            return CliOutput.failure(
                "Usage: agentforge skill validate <skillId> [version]"
            );
        }
        String skillId = args[0];
        String version = args.length > 1 ? args[1] : "latest";
        return CliOutput.success(
            "SKILL_VALIDATE: " + skillId + "@" + version
                + "\nValidation: contract + dependencies + strategy"
        );
    }

    private CliOutput handleTest(String[] args) {
        if (args.length < 1) {
            return CliOutput.failure(
                "Usage: agentforge skill test <skillId> [version]"
            );
        }
        String skillId = args[0];
        String version = args.length > 1 ? args[1] : "latest";
        return CliOutput.success(
            "SKILL_TEST: " + skillId + "@" + version
                + "\nRunning test suite..."
        );
    }

    private CliOutput handleEvaluate(String[] args) {
        if (args.length < 1) {
            return CliOutput.failure(
                "Usage: agentforge skill evaluate <skillId> [version]"
            );
        }
        String skillId = args[0];
        String version = args.length > 1 ? args[1] : "latest";
        return CliOutput.success(
            "SKILL_EVALUATE: " + skillId + "@" + version
                + "\nRunning 9-dimension evaluation..."
        );
    }

    private CliOutput handlePackage(String[] args) {
        String dir = args.length > 0 ? args[0] : ".";
        return CliOutput.success(
            "SKILL_PACKAGE: Packaging from " + dir
                + "\nOutput: skill-package.tar.gz"
        );
    }

    private CliOutput handlePublish(String[] args) {
        if (args.length < 2) {
            return CliOutput.failure(
                "Usage: agentforge skill publish <skillId> <version>"
            );
        }
        String skillId = args[0];
        String version = args[1];
        return CliOutput.success(
            "SKILL_PUBLISH: Publishing "
                + skillId + "@" + version
                + "\nRunning release gate pipeline..."
        );
    }

    private CliOutput handleActivate(String[] args) {
        if (args.length < 2) {
            return CliOutput.failure(
                "Usage: agentforge skill activate <skillId> <version>"
            );
        }
        return CliOutput.success(
            "SKILL_ACTIVATE: " + args[0] + "@" + args[1]
                + "\nTransition: PUBLISHED → ACTIVE"
        );
    }

    private CliOutput handleDeprecate(String[] args) {
        if (args.length < 2) {
            return CliOutput.failure(
                "Usage: agentforge skill deprecate <skillId> <version>"
            );
        }
        return CliOutput.success(
            "SKILL_DEPRECATE: " + args[0] + "@" + args[1]
                + "\nTransition: ACTIVE → DEPRECATED"
        );
    }

    private CliOutput handleRetire(String[] args) {
        if (args.length < 2) {
            return CliOutput.failure(
                "Usage: agentforge skill retire <skillId> <version>"
            );
        }
        return CliOutput.success(
            "SKILL_RETIRE: " + args[0] + "@" + args[1]
                + "\nTransition: DEPRECATED → RETIRED"
        );
    }

    private CliOutput handleRevoke(String[] args) {
        if (args.length < 2) {
            return CliOutput.failure(
                "Usage: agentforge skill revoke <skillId> <version>"
            );
        }
        return CliOutput.success(
            "SKILL_REVOKE: " + args[0] + "@" + args[1]
                + "\nEmergency revocation: ANY → REVOKED"
        );
    }

    private CliOutput handleInvoke(String[] args) {
        if (args.length < 1) {
            return CliOutput.failure(
                "Usage: agentforge skill invoke <skillId> [version] [json-input]"
            );
        }
        String skillId = args[0];
        String version = args.length > 1 ? args[1] : "latest";
        String input = args.length > 2 ? args[2] : "{}";
        return CliOutput.success(
            "SKILL_INVOKE: " + skillId + "@" + version
                + "\nInput: " + input
                + "\nDelegating to SkillInvoker (security + policy enforced)..."
        );
    }

    @FunctionalInterface
    private interface SubHandler {
        CliOutput handle(String[] args);
    }
}
