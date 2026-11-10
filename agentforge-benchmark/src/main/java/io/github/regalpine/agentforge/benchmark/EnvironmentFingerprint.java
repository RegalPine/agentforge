package io.github.regalpine.agentforge.benchmark;

import java.util.*;

/**
 * Environment fingerprint.
 * AF-ENHANCE-07 §4: captures the full
 * runtime environment so benchmark
 * results are reproducible.
 */
public record EnvironmentFingerprint(
    String javaVersion,
    String jvmVendor,
    String jvmName,
    String osName,
    String osArch,
    int availableProcessors,
    long maxMemoryMb,
    long totalMemoryMb,
    String agentforgeVersion,
    Map<String, String> extras
) {

    public EnvironmentFingerprint {
        extras = extras == null
            ? Map.of() : Map.copyOf(extras);
    }

    /**
     * Capture the current environment.
     */
    public static EnvironmentFingerprint capture() {
        Runtime rt = Runtime.getRuntime();
        return new EnvironmentFingerprint(
            System.getProperty("java.version", "unknown"),
            System.getProperty("java.vendor", "unknown"),
            System.getProperty("java.vm.name", "unknown"),
            System.getProperty("os.name", "unknown"),
            System.getProperty("os.arch", "unknown"),
            rt.availableProcessors(),
            rt.maxMemory() / (1024 * 1024),
            rt.totalMemory() / (1024 * 1024),
            "1.0.0-SNAPSHOT",
            Map.of()
        );
    }

    /**
     * Stable key for comparison.
     */
    public String stableKey() {
        return String.join("|",
            javaVersion, jvmName,
            osName, osArch,
            String.valueOf(availableProcessors),
            agentforgeVersion
        );
    }

    /**
     * Format as multi-line report.
     */
    public String report() {
        StringBuilder sb = new StringBuilder();
        sb.append("=== Environment Fingerprint ===\n");
        sb.append("Java:       ").append(javaVersion).append('\n');
        sb.append("JVM:        ").append(jvmVendor)
          .append(" / ").append(jvmName).append('\n');
        sb.append("OS:         ").append(osName)
          .append(" / ").append(osArch).append('\n');
        sb.append("CPUs:       ").append(availableProcessors).append('\n');
        sb.append("Max Memory: ").append(maxMemoryMb).append(" MB\n");
        sb.append("AgentForge: ").append(agentforgeVersion).append('\n');
        if (!extras.isEmpty()) {
            sb.append("Extras:\n");
            for (var e : extras.entrySet()) {
                sb.append("  ")
                  .append(e.getKey()).append(": ")
                  .append(e.getValue()).append('\n');
            }
        }
        return sb.toString();
    }
}
