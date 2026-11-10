package io.github.regalpine.agentforge.memory;

import java.time.Instant;
import java.util.Map;
import java.util.Objects;

/**
 * Memory candidate.
 * AE-08 §15: Memory Candidate.
 *
 * <p>A candidate is produced by the model
 * or extracted from observations. It has
 * not yet passed validation, policy or
 * deduplication. It is NOT a MemoryRecord.
 */
public record MemoryCandidate(
    String tenantId,
    String subjectId,
    MemoryType type,
    MemoryScope scope,
    String content,
    Map<String, Object> attributes,
    MemorySource source,
    MemoryConfidence confidence,
    MemoryImportance importance,
    MemoryProvenance provenance,
    Instant suggestedExpiresAt
) {

    public MemoryCandidate {
        Objects.requireNonNull(
            tenantId, "tenantId"
        );
        Objects.requireNonNull(
            subjectId, "subjectId"
        );
        Objects.requireNonNull(
            type, "type"
        );
        Objects.requireNonNull(
            scope, "scope"
        );
        Objects.requireNonNull(
            content, "content"
        );

        if (content.isBlank()) {
            throw new IllegalArgumentException(
                "Candidate content cannot be blank"
            );
        }

        attributes =
            attributes == null
                ? Map.of()
                : Map.copyOf(attributes);

        source =
            source == null
                ? MemorySource.AGENT_INFERRED
                : source;

        confidence =
            confidence == null
                ? MemoryConfidence.low(
                    ConfidenceBasis.AGENT_INFERENCE
                )
                : confidence;

        importance =
            importance == null
                ? MemoryImportance.NORMAL
                : importance;

        provenance =
            provenance == null
                ? MemoryProvenance.empty()
                : provenance;
    }
}
