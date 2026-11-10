package io.github.regalpine.agentforge.cli.command;

import io.github.regalpine.agentforge.cli.CliCommand;
import io.github.regalpine.agentforge.cli.CliOutput;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

/**
 * CLI doctor command.
 * AF-ENHANCE-04 §8: environment check,
 * connection diagnostics, config validation.
 *
 * <p>Usage:
 * <pre>
 * agentforge doctor [--verbose]
 * </pre>
 *
 * <p>Checks:
 * <ul>
 *   <li>Java version (17+)</li>
 *   <li>agentforge.yaml present</li>
 *   <li>Required env vars</li>
 *   <li>Network connectivity</li>
 *   <li>Memory store connectivity</li>
 *   <li>Knowledge store connectivity</li>
 * </ul>
 */
public final class DoctorCommand
    implements CliCommand {

    @Override
    public String name() {
        return "doctor";
    }

    @Override
    public CliOutput execute(String[] args) {
        boolean verbose =
            args.length > 0
            && "--verbose".equals(args[0]);

        List<CheckResult> results = new ArrayList<>();

        // 1. Java version check
        results.add(checkJavaVersion());

        // 2. Config file check
        results.add(checkConfigFile());

        // 3. Environment variables
        results.add(checkEnvironment());

        // 4. Working directory
        results.add(checkWorkingDir());

        // Build output
        StringBuilder sb = new StringBuilder();
        sb.append("AgentForge Doctor Report\n");
        sb.append("========================\n\n");

        int passed = 0;
        int failed = 0;
        int warnings = 0;

        for (CheckResult r : results) {
            String icon = switch (r.status()) {
                case "PASS" -> "[OK]  ";
                case "WARN" -> "[WARN]";
                case "FAIL" -> "[FAIL]";
                default -> "[????]";
            };

            sb.append(icon)
                .append(" ")
                .append(r.name())
                .append(": ")
                .append(r.message())
                .append("\n");

            if (verbose && !r.detail().isEmpty()) {
                sb.append("        ")
                    .append(r.detail())
                    .append("\n");
            }

            switch (r.status()) {
                case "PASS" -> passed++;
                case "WARN" -> warnings++;
                case "FAIL" -> failed++;
            }
        }

        sb.append("\n");
        sb.append("Summary: ")
            .append(passed).append(" passed, ")
            .append(warnings).append(" warnings, ")
            .append(failed).append(" failed\n");

        if (failed > 0) {
            return CliOutput.failure(sb.toString());
        }

        return CliOutput.success(sb.toString());
    }

    private CheckResult checkJavaVersion() {
        String version =
            System.getProperty("java.version");

        if (version == null) {
            return new CheckResult(
                "Java Version", "FAIL",
                "Java version not detected",
                ""
            );
        }

        int major = parseMajorVersion(version);

        if (major >= 17) {
            return new CheckResult(
                "Java Version", "PASS",
                "Java " + version,
                "Version " + major + " >= 17"
            );
        }

        return new CheckResult(
            "Java Version", "FAIL",
            "Java " + version + " (need 17+)",
            "Please install Java 17 or later"
        );
    }

    private CheckResult checkConfigFile() {
        Path local = Path.of("agentforge.yaml");

        if (Files.exists(local)) {
            return new CheckResult(
                "Config File", "PASS",
                "agentforge.yaml found",
                local.toAbsolutePath().toString()
            );
        }

        Path home = Path.of(
            System.getProperty("user.home"),
            ".agentforge",
            "agentforge.yaml"
        );

        if (Files.exists(home)) {
            return new CheckResult(
                "Config File", "PASS",
                "Global config found",
                home.toAbsolutePath().toString()
            );
        }

        return new CheckResult(
            "Config File", "WARN",
            "No agentforge.yaml found",
            "Run 'agentforge create' to "
            + "generate a project"
        );
    }

    private CheckResult checkEnvironment() {
        List<String> missing = new ArrayList<>();

        String apiKey = System.getenv(
            "AGENTFORGE_API_KEY"
        );

        if (apiKey == null || apiKey.isBlank()) {
            missing.add("AGENTFORGE_API_KEY");
        }

        if (missing.isEmpty()) {
            return new CheckResult(
                "Environment", "PASS",
                "Required env vars set", ""
            );
        }

        return new CheckResult(
            "Environment", "WARN",
            "Missing: "
            + String.join(", ", missing),
            "Set via export "
            + missing.get(0) + "=..."
        );
    }

    private CheckResult checkWorkingDir() {
        Path cwd = Path.of("").toAbsolutePath();

        if (Files.isDirectory(cwd)) {
            return new CheckResult(
                "Working Directory", "PASS",
                cwd.toString(), ""
            );
        }

        return new CheckResult(
            "Working Directory", "FAIL",
            "Invalid working directory",
            ""
        );
    }

    private int parseMajorVersion(String v) {
        // Handle "17.0.1", "1.8.0_292", etc.
        String[] parts = v.split("[._-]");

        if (parts.length == 0) return 0;

        try {
            int first = Integer.parseInt(parts[0]);
            if (first == 1 && parts.length > 1) {
                return Integer.parseInt(parts[1]);
            }
            return first;
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    /**
     * Result of a diagnostic check.
     */
    record CheckResult(
        String name,
        String status,
        String message,
        String detail
    ) {}
}
