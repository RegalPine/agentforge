package io.github.regalpine.agentforge.capability;

import java.util.Objects;

/**
 * 语义版本约束。
 *
 * <p>支持版本比较和兼容性判断。
 * 对应 AF-ENHANCE-05 §14。</p>
 */
public record SkillVersionConstraint(
    int major,
    int minor,
    int patch
) implements Comparable
    <SkillVersionConstraint> {

    public SkillVersionConstraint {
        if (major < 0
            || minor < 0
            || patch < 0
        ) {
            throw new IllegalArgumentException(
                "Version parts must be >= 0"
            );
        }
    }

    /**
     * 从字符串解析。
     */
    public static SkillVersionConstraint
    parse(String version) {
        Objects.requireNonNull(
            version, "version"
        );
        String[] parts = version.split("\\.");
        if (parts.length != 3) {
            throw new IllegalArgumentException(
                "Invalid version: " + version
            );
        }
        return new SkillVersionConstraint(
            Integer.parseInt(parts[0]),
            Integer.parseInt(parts[1]),
            Integer.parseInt(parts[2])
        );
    }

    /**
     * 是否兼容（同 major）。
     */
    public boolean isCompatibleWith(
        SkillVersionConstraint other
    ) {
        return this.major == other.major;
    }

    /**
     * 是否满足约束 >= required。
     */
    public boolean satisfies(
        SkillVersionConstraint required
    ) {
        return compareTo(required) >= 0;
    }

    @Override
    public int compareTo(
        SkillVersionConstraint o
    ) {
        int c = Integer.compare(
            major, o.major
        );
        if (c != 0) return c;
        c = Integer.compare(
            minor, o.minor
        );
        if (c != 0) return c;
        return Integer.compare(
            patch, o.patch
        );
    }

    @Override
    public String toString() {
        return major + "." + minor
            + "." + patch;
    }
}
