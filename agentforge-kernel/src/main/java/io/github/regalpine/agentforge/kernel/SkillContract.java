package io.github.regalpine.agentforge.kernel;

import java.util.Objects;

/**
 * Skill 输入输出契约.
 * ASM-SKILL-01 §31: SkillContract.
 *
 * <p>Immutable contract binding input schema,
 * output schema and error code set for a
 * Skill definition.
 */
public record SkillContract(
    Schema inputSchema,
    Schema outputSchema
) {

    public SkillContract {
        Objects.requireNonNull(
            inputSchema, "inputSchema"
        );
        Objects.requireNonNull(
            outputSchema, "outputSchema"
        );
    }

    public static SkillContract of(
        Schema inputSchema,
        Schema outputSchema
    ) {
        return new SkillContract(
            inputSchema, outputSchema
        );
    }
}
