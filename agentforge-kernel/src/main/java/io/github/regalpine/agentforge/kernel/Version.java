package io.github.regalpine.agentforge.kernel;

import java.util.Objects;

public record Version(String value)
    implements Comparable<Version> {

    public Version {
        Objects.requireNonNull(value, "value");

        if (!value.matches("\\d+\\.\\d+\\.\\d+")) {
            throw new IllegalArgumentException(
                "Version must follow MAJOR.MINOR.PATCH: " + value
            );
        }
    }

    /**
     * Parse major version number.
     */
    public int major() {
        return Integer.parseInt(
            value.split("\\.")[0]
        );
    }

    /**
     * Parse minor version number.
     */
    public int minor() {
        return Integer.parseInt(
            value.split("\\.")[1]
        );
    }

    /**
     * Parse patch version number.
     */
    public int patch() {
        return Integer.parseInt(
            value.split("\\.")[2]
        );
    }

    /**
     * Compare versions by MAJOR, MINOR, PATCH.
     * ASM-SKILL-01 §10: version ordering.
     */
    @Override
    public int compareTo(Version other) {
        int c = Integer.compare(
            major(), other.major()
        );
        if (c != 0) return c;

        c = Integer.compare(
            minor(), other.minor()
        );
        if (c != 0) return c;

        return Integer.compare(
            patch(), other.patch()
        );
    }

    /**
     * Check if this version is compatible
     * with another (same major version).
     * ASM-SKILL-01 §10.1: compatibility.
     */
    public boolean isCompatibleWith(Version other) {
        return major() == other.major();
    }

    /**
     * Check if this version is newer than another.
     */
    public boolean isNewerThan(Version other) {
        return compareTo(other) > 0;
    }

    @Override
    public String toString() {
        return value;
    }
}
