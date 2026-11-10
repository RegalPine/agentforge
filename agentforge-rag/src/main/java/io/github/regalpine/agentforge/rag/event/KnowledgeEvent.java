package io.github.regalpine.agentforge.rag.event;

import java.time.Instant;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Knowledge event.
 * AE-09 §66: Knowledge Events.
 */
public record KnowledgeEvent(
    KnowledgeEventType type,
    String tenantId,
    String sourceId,
    String documentId,
    Instant timestamp,
    Map<String, Object> metadata
) {

    public KnowledgeEvent {
        Objects.requireNonNull(type, "type");
        tenantId = tenantId == null ? "" : tenantId;
        sourceId = sourceId == null ? "" : sourceId;
        documentId = documentId == null ? "" : documentId;
        timestamp = timestamp == null ? Instant.now() : timestamp;
        metadata = metadata == null ? Map.of() : Map.copyOf(metadata);
    }

    public static KnowledgeEvent of(KnowledgeEventType type) {
        return new KnowledgeEvent(type, "", "", "", Instant.now(), Map.of());
    }

    public static KnowledgeEvent of(KnowledgeEventType type, String tenantId) {
        return new KnowledgeEvent(type, tenantId, "", "", Instant.now(), Map.of());
    }

    public Optional<Object> metadataValue(String key) {
        return Optional.ofNullable(metadata.get(key));
    }
}
