package io.github.regalpine.agentforge.sdk;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Metadata about an Agent.
 * AF-ENHANCE-01 §15: Agent API.
 *
 * <p>Provides descriptive information
 * about an Agent's identity, version,
 * and capabilities.
 *
 * <p>Immutable. Thread-safe.
 */
public final class AgentMetadata {

    private final String id;
    private final String name;
    private final String version;
    private final String description;
    private final Map<String, String> labels;

    public AgentMetadata(
        String id,
        String name,
        String version,
        String description,
        Map<String, String> labels
    ) {
        this.id = Objects.requireNonNull(id, "id");
        this.name = name != null ? name : id;
        this.version = version != null ? version : "1.0.0";
        this.description = description != null
            ? description : "";
        this.labels = labels != null
            ? Map.copyOf(labels) : Map.of();
    }

    public AgentMetadata(String id, String name) {
        this(id, name, "1.0.0", "", Map.of());
    }

    public String id() {
        return id;
    }

    public String name() {
        return name;
    }

    public String version() {
        return version;
    }

    public String description() {
        return description;
    }

    public Map<String, String> labels() {
        return labels;
    }

    /**
     * Create a new metadata builder.
     */
    public static Builder builder(String id) {
        return new Builder(id);
    }

    /**
     * Builder for AgentMetadata.
     */
    public static final class Builder {

        private final String id;
        private String name;
        private String version = "1.0.0";
        private String description = "";
        private final Map<String, String> labels =
            new LinkedHashMap<>();

        Builder(String id) {
            this.id = Objects.requireNonNull(id, "id");
        }

        public Builder name(String name) {
            this.name = name;
            return this;
        }

        public Builder version(String version) {
            this.version = version;
            return this;
        }

        public Builder description(String desc) {
            this.description = desc;
            return this;
        }

        public Builder label(String key, String value) {
            labels.put(key, value);
            return this;
        }

        public AgentMetadata build() {
            return new AgentMetadata(
                id, name, version,
                description, labels
            );
        }
    }
}
