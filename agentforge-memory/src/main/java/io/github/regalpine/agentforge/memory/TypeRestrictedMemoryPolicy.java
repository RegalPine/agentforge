package io.github.regalpine.agentforge.memory;

import java.util.Objects;
import java.util.Set;

/**
 * Type-restricted memory policy.
 * AE-08 §17: restricts which memory
 * types can be read/written.
 *
 * <p>Useful for enforcing tenant-level
 * or agent-level restrictions on
 * memory operations.
 */
public final class TypeRestrictedMemoryPolicy
    implements MemoryPolicy {

    private final Set<MemoryType> allowedReadTypes;
    private final Set<MemoryType> allowedWriteTypes;
    private final boolean allowCrossSession;

    public TypeRestrictedMemoryPolicy(
        Set<MemoryType> allowedReadTypes,
        Set<MemoryType> allowedWriteTypes,
        boolean allowCrossSession
    ) {
        this.allowedReadTypes =
            allowedReadTypes == null
                ? Set.of(MemoryType.values())
                : Set.copyOf(allowedReadTypes);
        this.allowedWriteTypes =
            allowedWriteTypes == null
                ? Set.of(MemoryType.values())
                : Set.copyOf(allowedWriteTypes);
        this.allowCrossSession =
            allowCrossSession;
    }

    @Override
    public MemoryDecision evaluateRead(
        MemoryQuery query,
        MemoryAccessContext context
    ) {
        if (query.type() != null
            && !allowedReadTypes.contains(
                query.type()
            )
        ) {
            return MemoryDecision.deny(
                "Memory type " + query.type()
                    + " not allowed for reading"
            );
        }

        return MemoryDecision.allow();
    }

    @Override
    public MemoryDecision evaluateWrite(
        MemoryCandidate candidate,
        MemoryAccessContext context
    ) {
        if (!allowedWriteTypes.contains(
            candidate.type()
        )) {
            return MemoryDecision.deny(
                "Memory type " + candidate.type()
                    + " not allowed for writing"
            );
        }

        // AGENT_INFERRED memories should not
        // be promoted to USER scope without
        // explicit approval
        if (candidate.source()
                == MemorySource.AGENT_INFERRED
            && candidate.scope()
                == MemoryScope.USER
        ) {
            return MemoryDecision.denyApproval(
                "Agent-inferred USER memory "
                    + "requires approval"
            );
        }

        return MemoryDecision.allow();
    }

    /**
     * Builder for TypeRestrictedMemoryPolicy.
     */
    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {

        private Set<MemoryType> readTypes;
        private Set<MemoryType> writeTypes;
        private boolean crossSession = true;

        private Builder() {}

        public Builder allowedReadTypes(
            Set<MemoryType> types
        ) {
            this.readTypes = types;
            return this;
        }

        public Builder allowedWriteTypes(
            Set<MemoryType> types
        ) {
            this.writeTypes = types;
            return this;
        }

        public Builder allowCrossSession(
            boolean allow
        ) {
            this.crossSession = allow;
            return this;
        }

        public TypeRestrictedMemoryPolicy build() {
            return new TypeRestrictedMemoryPolicy(
                readTypes, writeTypes, crossSession
            );
        }
    }
}
