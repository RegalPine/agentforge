package io.github.regalpine.agentforge.multiagent.shared;

import java.time.Instant;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * SharedContext —
 * AF-ENHANCE-08 §37, §73.
 *
 * Explicitly authorized shared
 * context between agents.
 *
 * <p>Default: Agent A Context
 * ≠ Agent B Context.
 *
 * <p>Sharing must include:
 * source, classification,
 * provenance, scope, expiration.
 */
public record SharedContext(

    String contextId,

    String sourceAgentId,

    Set<String> targetAgentIds,

    Map<String, Object> items,

    String classification,

    String provenance,

    String scope,

    Instant expiresAt,

    Instant createdAt

) {

    public SharedContext {
        Objects.requireNonNull(
            contextId, "contextId"
        );
        Objects.requireNonNull(
            sourceAgentId, "sourceAgentId"
        );

        targetAgentIds =
            targetAgentIds == null
                ? Set.of()
                : Set.copyOf(targetAgentIds);

        items = items == null
            ? Map.of() : Map.copyOf(items);

        classification =
            classification == null
                ? "INTERNAL"
                : classification;

        scope = scope == null
            ? "INVOCATION" : scope;

        createdAt = createdAt == null
            ? Instant.now() : createdAt;
    }

    /**
     * Create a shared context.
     */
    public static SharedContext create(
        String sourceAgentId,
        Set<String> targetAgentIds,
        Map<String, Object> items
    ) {
        return new SharedContext(
            "ctx-" + System.nanoTime(),
            sourceAgentId,
            targetAgentIds, items,
            null, null, null,
            null, null
        );
    }

    /**
     * Check if an agent is a target.
     */
    public boolean isAccessibleBy(
        String agentId
    ) {
        return targetAgentIds.isEmpty()
            || targetAgentIds.contains(agentId);
    }

    /**
     * Check if context has expired.
     */
    public boolean isExpired() {
        return expiresAt != null
            && Instant.now().isAfter(expiresAt);
    }
}
