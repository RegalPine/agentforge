package io.github.regalpine.agentforge.memory;

/**
 * Memory policy.
 * AE-08 §17-18: Memory Policy.
 *
 * <p>Controls read/write access to memory.
 * Policy must be evaluated before any
 * memory operation. Memory cannot bypass
 * policy.
 */
public interface MemoryPolicy {

    /**
     * Evaluate whether a memory read
     * is allowed for the given query.
     */
    MemoryDecision evaluateRead(
        MemoryQuery query,
        MemoryAccessContext context
    );

    /**
     * Evaluate whether a memory write
     * is allowed for the given candidate.
     */
    MemoryDecision evaluateWrite(
        MemoryCandidate candidate,
        MemoryAccessContext context
    );

    /**
     * Permissive policy that allows
     * all operations. For development
     * and testing only.
     */
    static MemoryPolicy permissive() {
        return new MemoryPolicy() {
            @Override
            public MemoryDecision evaluateRead(
                MemoryQuery query,
                MemoryAccessContext context
            ) {
                return MemoryDecision.allow();
            }

            @Override
            public MemoryDecision evaluateWrite(
                MemoryCandidate candidate,
                MemoryAccessContext context
            ) {
                return MemoryDecision.allow();
            }
        };
    }

    /**
     * Deny-all policy. No reads or writes.
     */
    static MemoryPolicy denyAll() {
        return new MemoryPolicy() {
            @Override
            public MemoryDecision evaluateRead(
                MemoryQuery query,
                MemoryAccessContext context
            ) {
                return MemoryDecision.deny(
                    "All reads denied by policy"
                );
            }

            @Override
            public MemoryDecision evaluateWrite(
                MemoryCandidate candidate,
                MemoryAccessContext context
            ) {
                return MemoryDecision.deny(
                    "All writes denied by policy"
                );
            }
        };
    }
}
