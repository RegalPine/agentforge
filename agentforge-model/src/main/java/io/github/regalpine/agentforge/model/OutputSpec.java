package io.github.regalpine.agentforge.model;

import java.util.Objects;

/**
 * Specification for structured output.
 */
public record OutputSpec(
    String name,
    OutputSpecMode mode,
    String schema,
    Class<?> targetType,
    OutputStrictness strictness,
    RepairPolicy repairPolicy
) {

    public OutputSpec {
        Objects.requireNonNull(name, "name");
        Objects.requireNonNull(mode, "mode");
        Objects.requireNonNull(strictness, "strictness");

        if (name.isBlank()) {
            throw new IllegalArgumentException("name cannot be blank");
        }
    }

    /**
     * Create a JSON schema output spec.
     *
     * @param name the output name
     * @param schema the JSON schema
     * @return a new OutputSpec
     */
    public static OutputSpec jsonSchema(String name, String schema) {
        return new OutputSpec(name, OutputSpecMode.JSON_SCHEMA, schema, null, OutputStrictness.STRICT, null);
    }

    /**
     * Create a typed output spec.
     *
     * @param name the output name
     * @param targetType the target Java type
     * @return a new OutputSpec
     */
    public static OutputSpec typed(String name, Class<?> targetType) {
        return new OutputSpec(name, OutputSpecMode.TYPED_OBJECT, null, targetType, OutputStrictness.STRICT, null);
    }

    /**
     * Create an enum output spec.
     *
     * @param name the output name
     * @param enumType the enum class
     * @return a new OutputSpec
     */
    public static OutputSpec enumSpec(String name, Class<?> enumType) {
        if (!enumType.isEnum()) {
            throw new IllegalArgumentException("enumType must be an enum: " + enumType);
        }
        return new OutputSpec(name, OutputSpecMode.ENUM, null, enumType, OutputStrictness.STRICT, null);
    }
}
