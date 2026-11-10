package io.github.regalpine.agentforge.evolution;

/**
 * Evidence reference.
 * AF-ENHANCE-15 §23: Evidence.
 *
 * <p>Evidence links a candidate
 * to its supporting data.
 */
public record EvidenceReference(
    EvidenceType type,
    String id,
    String digest
) {

    public EvidenceReference {
        if (type == null) {
            throw new IllegalArgumentException(
                "type required");
        }
        if (id == null
            || id.isBlank()) {
            throw new IllegalArgumentException(
                "id required");
        }
    }

    /**
     * Create evidence reference.
     */
    public static EvidenceReference of(
        EvidenceType type,
        String id
    ) {
        return new EvidenceReference(
            type, id, null);
    }
}
