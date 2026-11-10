package io.github.regalpine.agentforge.graph.ir;

import io.github.regalpine.agentforge.kernel.SkillReference;

import java.util.Map;
import java.util.Objects;

/**
 * Skill Node in Graph IR.
 * AF-SKILL-01 §27: SkillNode.
 * AF-AD-15 §7.2: Graph Skill Node.
 *
 * <p>Represents a Skill invocation within
 * a Graph execution plan. Carries the skill
 * reference and input/output mappings.
 * Immutable.
 */
public record SkillNode(
    SkillReference skill,
    Map<String, Object> inputMapping,
    Map<String, Object> outputMapping
) {

    public SkillNode {
        Objects.requireNonNull(skill, "skill");

        inputMapping =
            inputMapping == null
                ? Map.of()
                : Map.copyOf(inputMapping);

        outputMapping =
            outputMapping == null
                ? Map.of()
                : Map.copyOf(outputMapping);
    }
}
