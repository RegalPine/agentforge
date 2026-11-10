package io.github.regalpine.agentforge.sdk;

import java.util.Map;
import java.util.Objects;

/**
 * Reference to a registered Skill.
 * AF-ENHANCE-01 §14: AgentBuilder.skill().
 * AF-ENHANCE-05: Skill Runtime (X4 phase).
 *
 * <p>Lightweight reference that identifies
 * a Skill by namespace:name:version.
 */
public record SkillRef(
    String namespace,
    String name,
    String version
) {

    public SkillRef {
        Objects.requireNonNull(name, "name");
    }

    /**
     * Create a skill reference with just
     * a name (default namespace).
     */
    public static SkillRef of(String name) {
        return new SkillRef("default", name, "1.0.0");
    }

    /**
     * Create a skill reference with
     * name and version.
     */
    public static SkillRef of(
        String name, String version
    ) {
        return new SkillRef(
            "default", name, version
        );
    }

    /**
     * Full qualified identifier:
     * namespace:name:version.
     */
    public String qualifiedId() {
        return namespace + ":" + name
            + ":" + version;
    }
}
