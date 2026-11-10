package io.github.regalpine.agentforge.workspace;

import java.time.Duration;
import java.util.Objects;

/**
 * Resource limits for sandbox.
 * AF-ENHANCE-07 §38: Resource Limits.
 *
 * <p>CPU, Memory, Disk, File Count,
 * Process Count, Network, Execution
 * Time, Output Size.
 */
public record ResourceLimits(
    int cpuCores,
    long memoryMb,
    long diskMb,
    int maxFiles,
    int maxProcesses,
    Duration timeout,
    long maxOutputMb
) {

    public ResourceLimits {
        if (cpuCores <= 0) {
            cpuCores = 1;
        }
        if (memoryMb <= 0) {
            memoryMb = 512;
        }
        if (diskMb <= 0) {
            diskMb = 1024;
        }
        if (maxFiles <= 0) {
            maxFiles = 1000;
        }
        if (maxProcesses <= 0) {
            maxProcesses = 4;
        }
        if (timeout == null
            || timeout.isNegative()
            || timeout.isZero()
        ) {
            timeout = Duration.ofSeconds(120);
        }
        if (maxOutputMb <= 0) {
            maxOutputMb = 100;
        }
    }

    /**
     * Default resource limits.
     */
    public static ResourceLimits defaults() {
        return new ResourceLimits(
            1, 512, 1024,
            1000, 4,
            Duration.ofSeconds(120),
            100
        );
    }

    /**
     * Small limits for testing.
     */
    public static ResourceLimits small() {
        return new ResourceLimits(
            1, 128, 256,
            100, 2,
            Duration.ofSeconds(30),
            10
        );
    }

    /**
     * Large limits for production.
     */
    public static ResourceLimits large() {
        return new ResourceLimits(
            4, 4096, 8192,
            10000, 16,
            Duration.ofSeconds(600),
            1024
        );
    }

    /**
     * Check if a memory request
     * is within limits.
     */
    public boolean isMemoryWithin(long mb) {
        return mb <= memoryMb;
    }

    /**
     * Check if a file count is
     * within limits.
     */
    public boolean isFileCountWithin(int count) {
        return count <= maxFiles;
    }
}
