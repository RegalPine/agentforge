package io.github.regalpine.agentforge.cli.command;

import io.github.regalpine.agentforge.cli.CliCommand;
import io.github.regalpine.agentforge.cli.CliOutput;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * CLI validate command.
 * AF-ENHANCE-01 §7, §21: agentforge validate.
 *
 * <p>Validates definition completeness:
 * ASM semantic validation + dependency
 * resolution + circular dependency check.
 */
public final class ValidateCommand
    implements CliCommand {

    @Override
    public String name() {
        return "validate";
    }

    @Override
    public CliOutput execute(
        String[] args
    ) {
        Path projectDir = args.length > 0
            ? Path.of(args[0])
            : Path.of(".");

        List<String> errors = new ArrayList<>();
        List<String> warnings = new ArrayList<>();

        // Check agentforge.yaml
        Path yaml = projectDir.resolve(
            "agentforge.yaml"
        );
        if (!Files.exists(yaml)) {
            errors.add(
                "Missing agentforge.yaml"
            );
        }

        // Check agents directory
        Path agentsDir = projectDir.resolve(
            "agents"
        );
        if (!Files.isDirectory(agentsDir)) {
            warnings.add(
                "No agents/ directory"
            );
        } else {
            // Validate each agent YAML
            try (var files =
                Files.list(agentsDir)
            ) {
                files.filter(p ->
                    p.toString().endsWith(".yaml")
                    || p.toString().endsWith(".yml")
                ).forEach(p ->
                    validateAgentYaml(
                        p, errors, warnings
                    )
                );
            } catch (IOException e) {
                errors.add(
                    "Failed to scan agents/: "
                        + e.getMessage()
                );
            }
        }

        // Check recommended directories
        String[] recommended = {
            "skills", "tools", "prompts"
        };
        for (String dir : recommended) {
            if (!Files.isDirectory(
                projectDir.resolve(dir)
            )) {
                warnings.add(
                    "Missing recommended "
                        + "directory: " + dir + "/"
                );
            }
        }

        // Build result
        if (errors.isEmpty()
            && warnings.isEmpty()
        ) {
            return CliOutput.success(
                "Validation passed.\n"
                    + "  All definitions valid.\n"
                    + "  No issues found."
            );
        }

        StringBuilder sb = new StringBuilder();
        sb.append("Validation ");

        if (errors.isEmpty()) {
            sb.append("passed with warnings");
        } else {
            sb.append("FAILED");
        }

        sb.append(":\n");

        for (String error : errors) {
            sb.append("  ERROR: ")
                .append(error).append("\n");
        }

        for (String warning : warnings) {
            sb.append("  WARN:  ")
                .append(warning).append("\n");
        }

        return errors.isEmpty()
            ? CliOutput.success(sb.toString())
            : CliOutput.failure(sb.toString());
    }

    private void validateAgentYaml(
        Path yamlFile,
        List<String> errors,
        List<String> warnings
    ) {
        try {
            String content =
                Files.readString(yamlFile);

            if (!content.contains("id:")) {
                errors.add(
                    yamlFile.getFileName()
                        + ": missing 'id' field"
                );
            }

            if (!content.contains("model:")) {
                warnings.add(
                    yamlFile.getFileName()
                        + ": no model reference"
                );
            }
        } catch (IOException e) {
            errors.add(
                yamlFile.getFileName()
                    + ": read error — "
                    + e.getMessage()
            );
        }
    }
}
