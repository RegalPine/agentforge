package io.github.regalpine.agentforge.sdk.bootstrap;

import java.util.List;
import java.util.Objects;

/**
 * Parsed Agent definition.
 * AF-ENHANCE-01 §12: Agent Definition.
 *
 * <p>Represents the content of an
 * agent YAML file before it is
 * mapped to the ASM model.
 *
 * <p>Immutable.
 */
public record AgentDefinition(
    String id,
    String name,
    String modelRef,
    List<String> skills,
    List<String> tools
) {

    public AgentDefinition {
        Objects.requireNonNull(id, "id");
        skills = skills != null
            ? List.copyOf(skills) : List.of();
        tools = tools != null
            ? List.copyOf(tools) : List.of();
    }

    /**
     * Check if this definition has a
     * model reference.
     */
    public boolean hasModel() {
        return modelRef != null
            && !modelRef.isBlank();
    }

    /**
     * Check if this definition has
     * any skills.
     */
    public boolean hasSkills() {
        return !skills.isEmpty();
    }

    /**
     * Check if this definition has
     * any tools.
     */
    public boolean hasTools() {
        return !tools.isEmpty();
    }
}
