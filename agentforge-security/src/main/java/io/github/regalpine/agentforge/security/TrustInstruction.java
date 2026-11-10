package io.github.regalpine.agentforge.security;

import java.util.Map;
import java.util.Objects;

/**
 * An instruction segment with a trust level.
 * <p>
 * Used to enforce the Instruction Hierarchy — higher trust instructions
 * take precedence over lower trust ones.
 */
public record TrustInstruction(
    InstructionTrustLevel trustLevel,
    String content,
    Map<String, Object> metadata
) {

    public TrustInstruction {
        Objects.requireNonNull(trustLevel, "trustLevel cannot be null");
        Objects.requireNonNull(content, "content cannot be null");
        metadata = metadata == null ? Map.of() : Map.copyOf(metadata);
    }

    public static TrustInstruction system(String content) {
        return new TrustInstruction(InstructionTrustLevel.SYSTEM, content, null);
    }

    public static TrustInstruction user(String content) {
        return new TrustInstruction(InstructionTrustLevel.USER, content, null);
    }

    public static TrustInstruction external(String content) {
        return new TrustInstruction(InstructionTrustLevel.EXTERNAL, content, null);
    }

    public static TrustInstruction tool(String content) {
        return new TrustInstruction(InstructionTrustLevel.TOOL, content, null);
    }

    public static TrustInstruction knowledge(String content) {
        return new TrustInstruction(InstructionTrustLevel.KNOWLEDGE, content, null);
    }
}
