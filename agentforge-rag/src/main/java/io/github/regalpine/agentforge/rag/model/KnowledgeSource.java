package io.github.regalpine.agentforge.rag.model;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Knowledge source.
 * AE-09 §6: Knowledge Source.
 *
 * <p>A registered external knowledge resource from which documents
 * are discovered and ingested.
 */
public final class KnowledgeSource {

    private final String sourceId;
    private final String tenantId;
    private final String name;
    private final KnowledgeSourceType type;
    private final String version;
    private final KnowledgeSourceStatus status;
    private final KnowledgeAuthority authority;
    private final Map<String, Object> metadata;

    public KnowledgeSource(
            String sourceId,
            String tenantId,
            String name,
            KnowledgeSourceType type,
            String version,
            KnowledgeSourceStatus status,
            KnowledgeAuthority authority,
            Map<String, Object> metadata) {
        this.sourceId = Objects.requireNonNull(sourceId, "sourceId");
        this.tenantId = Objects.requireNonNull(tenantId, "tenantId");
        this.name = Objects.requireNonNull(name, "name");
        this.type = Objects.requireNonNull(type, "type");
        this.version = Objects.requireNonNull(version, "version");
        this.status = Objects.requireNonNull(status, "status");
        this.authority = Objects.requireNonNull(authority, "authority");
        this.metadata = metadata == null ? Map.of() : Map.copyOf(metadata);
    }

    public String sourceId() { return sourceId; }
    public String tenantId() { return tenantId; }
    public String name() { return name; }
    public KnowledgeSourceType type() { return type; }
    public String version() { return version; }
    public KnowledgeSourceStatus status() { return status; }
    public KnowledgeAuthority authority() { return authority; }
    public Map<String, Object> metadata() { return metadata; }

    public Optional<Object> metadataValue(String key) {
        return Optional.ofNullable(metadata.get(key));
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof KnowledgeSource that)) return false;
        return sourceId.equals(that.sourceId);
    }

    @Override
    public int hashCode() {
        return sourceId.hashCode();
    }

    @Override
    public String toString() {
        return "KnowledgeSource{id=" + sourceId + ", name=" + name + ", type=" + type + ", status=" + status + "}";
    }
}
