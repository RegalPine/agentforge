package io.github.regalpine.agentforge.cli.command;

import io.github.regalpine.agentforge.cli.CliCommand;
import io.github.regalpine.agentforge.cli.CliOutput;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;

/**
 * CLI inspect command.
 * AF-ENHANCE-01 §7: agentforge inspect.
 *
 * <p>Inspects ASM and Runtime state.
 * Shows registered agents, skills,
 * tools, models, and policies.
 */
public final class InspectCommand
    implements CliCommand {

    @Override
    public String name() {
        return "inspect";
    }

    @Override
    public CliOutput execute(
        String[] args
    ) {
        String target = args.length > 0
            ? args[0] : "all";

        Path projectDir = Path.of(".");

        if (!Files.exists(
            projectDir.resolve("agentforge.yaml")
        )) {
            return CliOutput.failure(
                "Not an AgentForge project.\n"
                    + "Run from project root."
            );
        }

        StringBuilder sb = new StringBuilder();
        sb.append("AgentForge Inspection\n");
        sb.append("=====================\n\n");

        // Inspect agents
        if ("all".equals(target)
            || "agents".equals(target)
        ) {
            sb.append(inspectAgents(projectDir));
        }

        // Inspect skills
        if ("all".equals(target)
            || "skills".equals(target)
        ) {
            sb.append(inspectSkills(projectDir));
        }

        // Inspect tools
        if ("all".equals(target)
            || "tools".equals(target)
        ) {
            sb.append(inspectDir(
                "Tools",
                projectDir.resolve("tools")
            ));
        }

        // Inspect policies
        if ("all".equals(target)
            || "policies".equals(target)
        ) {
            sb.append(inspectDir(
                "Policies",
                projectDir.resolve("policies")
            ));
        }

        return CliOutput.success(
            sb.toString()
        );
    }

    private String inspectAgents(
        Path projectDir
    ) {
        Path agentsDir =
            projectDir.resolve("agents");
        StringBuilder sb = new StringBuilder();
        sb.append("Agents:\n");

        if (!Files.isDirectory(agentsDir)) {
            sb.append("  (none)\n\n");
            return sb.toString();
        }

        try (Stream<Path> files =
            Files.list(agentsDir)
        ) {
            files.filter(p ->
                p.toString().endsWith(".yaml")
                || p.toString().endsWith(".yml")
            ).forEach(p ->
                sb.append("  - ")
                    .append(p.getFileName())
                    .append("\n")
            );
        } catch (IOException e) {
            sb.append("  (error: ")
                .append(e.getMessage())
                .append(")\n");
        }

        sb.append("\n");
        return sb.toString();
    }

    private String inspectSkills(
        Path projectDir
    ) {
        return inspectDir(
            "Skills",
            projectDir.resolve("skills")
        );
    }

    private String inspectDir(
        String label, Path dir
    ) {
        StringBuilder sb = new StringBuilder();
        sb.append(label).append(":\n");

        if (!Files.isDirectory(dir)) {
            sb.append("  (none)\n\n");
            return sb.toString();
        }

        try (Stream<Path> files =
            Files.list(dir)
        ) {
            long count = files.count();
            if (count == 0) {
                sb.append("  (empty)\n");
            } else {
                sb.append("  ")
                    .append(count)
                    .append(" item(s)\n");
            }
        } catch (IOException e) {
            sb.append("  (error: ")
                .append(e.getMessage())
                .append(")\n");
        }

        sb.append("\n");
        return sb.toString();
    }
}
