package io.github.regalpine.agentforge.provider.mcp;

import java.net.URI;
import java.util.Map;
import java.util.Objects;

/**
 * MCP server definition.
 * AF-ENHANCE-10 §14: Server Definition.
 *
 * <p>Only stores Identity, Endpoint,
 * Transport, Credential Reference,
 * Trust, and Policy. Does NOT store
 * secrets, session state, or
 * execution state.
 */
public record McpServerDefinition(
    McpServerId serverId,
    String endpoint,
    McpTransportType transport,
    String credentialReference,
    McpServerTrust trust,
    String policyReference,
    Map<String, Object> metadata
) {

    public McpServerDefinition {
        Objects.requireNonNull(
            serverId, "serverId"
        );
        Objects.requireNonNull(
            endpoint, "endpoint"
        );
        Objects.requireNonNull(
            transport, "transport"
        );

        credentialReference =
            credentialReference == null
                ? "" : credentialReference;
        trust = trust == null
            ? McpServerTrust.UNTRUSTED
            : trust;
        policyReference =
            policyReference == null
                ? "" : policyReference;
        metadata = metadata == null
            ? Map.of() : Map.copyOf(metadata);
    }

    /**
     * Create a builder.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Create with minimal fields.
     */
    public static McpServerDefinition of(
        String serverId,
        String endpoint,
        McpTransportType transport
    ) {
        return new McpServerDefinition(
            new McpServerId(serverId),
            endpoint, transport,
            null, null, null, null
        );
    }

    public static final class Builder {

        private McpServerId serverId;
        private String endpoint;
        private McpTransportType transport;
        private String credentialReference;
        private McpServerTrust trust;
        private String policyReference;
        private Map<String, Object> metadata;

        private Builder() {}

        public Builder serverId(
            McpServerId serverId
        ) {
            this.serverId = serverId;
            return this;
        }

        public Builder serverId(
            String serverId
        ) {
            this.serverId =
                new McpServerId(serverId);
            return this;
        }

        public Builder endpoint(
            String endpoint
        ) {
            this.endpoint = endpoint;
            return this;
        }

        public Builder transport(
            McpTransportType transport
        ) {
            this.transport = transport;
            return this;
        }

        public Builder credentialReference(
            String ref
        ) {
            this.credentialReference = ref;
            return this;
        }

        public Builder trust(
            McpServerTrust trust
        ) {
            this.trust = trust;
            return this;
        }

        public Builder policyReference(
            String ref
        ) {
            this.policyReference = ref;
            return this;
        }

        public Builder metadata(
            Map<String, Object> metadata
        ) {
            this.metadata = metadata;
            return this;
        }

        public McpServerDefinition build() {
            return new McpServerDefinition(
                serverId, endpoint, transport,
                credentialReference, trust,
                policyReference, metadata
            );
        }
    }
}
