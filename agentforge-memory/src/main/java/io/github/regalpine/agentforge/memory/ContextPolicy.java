package io.github.regalpine.agentforge.memory;

import java.util.Objects;
import java.util.Set;

/**
 * Context policy.
 * AF-ENHANCE-06 §41: Context Policy.
 *
 * <p>Controls what can enter the
 * assembled context: allowed memory
 * types, max memory count, max tokens,
 * allowed classifications, whether
 * workspace/conversation/tool results
 * are permitted.
 */
public record ContextPolicy(
    String policyId,
    Set<MemoryType> allowedMemoryTypes,
    int maxMemoryCount,
    int maxContextTokens,
    boolean allowConversation,
    boolean allowWorkspace,
    boolean allowToolResults,
    boolean allowKnowledge,
    Set<ContextPriority> minPriority
) {

    public ContextPolicy {
        Objects.requireNonNull(
            policyId, "policyId"
        );

        allowedMemoryTypes =
            allowedMemoryTypes == null
                ? Set.copyOf(
                    Set.of(MemoryType.values())
                )
                : Set.copyOf(
                    allowedMemoryTypes
                );

        if (maxMemoryCount <= 0) {
            maxMemoryCount = 20;
        }
        if (maxContextTokens <= 0) {
            maxContextTokens = 32_000;
        }

        minPriority =
            minPriority == null
                ? Set.of(
                    ContextPriority.values()
                )
                : Set.copyOf(minPriority);
    }

    /**
     * Permissive context policy
     * allowing everything.
     */
    public static ContextPolicy permissive() {
        return new ContextPolicy(
            "permissive",
            null, 100, 128_000,
            true, true, true, true,
            null
        );
    }

    /**
     * Restrictive context policy
     * for high-security scenarios.
     */
    public static ContextPolicy restrictive() {
        return new ContextPolicy(
            "restrictive",
            Set.of(
                MemoryType.FACT,
                MemoryType.SUMMARY
            ),
            5, 8_000,
            false, false, false, false,
            Set.of(
                ContextPriority.CRITICAL,
                ContextPriority.HIGH
            )
        );
    }

    /**
     * Check if a memory type is
     * allowed by this policy.
     */
    public boolean isMemoryTypeAllowed(
        MemoryType type
    ) {
        return allowedMemoryTypes.contains(
            type
        );
    }

    /**
     * Check if a priority level is
     * above the minimum threshold.
     */
    public boolean isPriorityAllowed(
        ContextPriority priority
    ) {
        return minPriority.contains(
            priority
        );
    }
}
