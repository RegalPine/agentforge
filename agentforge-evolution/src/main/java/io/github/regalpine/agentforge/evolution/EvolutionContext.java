package io.github.regalpine.agentforge.evolution;

import java.util.Map;

/**
 * Evolution execution context.
 * AF-ENHANCE-15 §29: Candidate
 * Generator.
 */
public record EvolutionContext(
    String agentReference,
    String agentVersion,
    EvolutionPolicy policy,
    Map<String, String> attributes
) {

    public EvolutionContext {
        if (agentReference == null
            || agentReference.isBlank()) {
            throw new IllegalArgumentException(
                "agentReference required");
        }
        attributes = attributes == null
            ? Map.of()
            : Map.copyOf(attributes);
    }

    /**
     * Create minimal context.
     */
    public static EvolutionContext of(
        String agentReference,
        String agentVersion
    ) {
        return new EvolutionContext(
            agentReference,
            agentVersion,
            null, Map.of()
        );
    }
}
