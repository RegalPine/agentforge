package io.github.regalpine.agentforge.multiagent.shared;

import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * SharedWorkspace —
 * AF-ENHANCE-08 §39.
 *
 * Explicitly authorized shared
 * workspace between agents.
 *
 * <p>Default: Shared Workspace = DENY.
 * Must be explicitly declared.
 *
 * <p>Conflict resolution:
 * LOCK, VERSION, LAST_WRITE_REJECT,
 * COPY_ON_WRITE.
 * Default: VERSION.
 */
public record SharedWorkspace(

    String workspaceId,

    Set<String> participantAgentIds,

    ConflictResolution conflictResolution,

    boolean readWrite,

    Map<String, Object> metadata

) {

    public SharedWorkspace {
        Objects.requireNonNull(
            workspaceId, "workspaceId"
        );

        participantAgentIds =
            participantAgentIds == null
                ? Set.of()
                : Set.copyOf(participantAgentIds);

        conflictResolution =
            conflictResolution == null
                ? ConflictResolution.VERSION
                : conflictResolution;

        metadata = metadata == null
            ? Map.of() : Map.copyOf(metadata);
    }

    /**
     * Create a shared workspace.
     */
    public static SharedWorkspace create(
        String workspaceId,
        Set<String> participants
    ) {
        return new SharedWorkspace(
            workspaceId, participants,
            null, true, null
        );
    }

    /**
     * Check if an agent can access.
     */
    public boolean isAccessibleBy(
        String agentId
    ) {
        return participantAgentIds.isEmpty()
            || participantAgentIds.contains(
                agentId
            );
    }

    /**
     * Conflict resolution strategy.
     * AF-ENHANCE-08 §40.
     */
    public enum ConflictResolution {
        /**
         * Lock-based.
         */
        LOCK,

        /**
         * Version-based.
         * Default.
         */
        VERSION,

        /**
         * Last write rejects
         * conflicting writes.
         */
        LAST_WRITE_REJECT,

        /**
         * Copy on write.
         */
        COPY_ON_WRITE
    }
}
