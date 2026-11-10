package io.github.regalpine.agentforge.runtime.activation;

import io.github.regalpine.agentforge.kernel.EntityId;

import java.util.*;

/**
 * Tracks the collection of tokens for a join node.
 * When all expected branches have produced tokens, the join is complete.
 *
 * <p>G-01: Join frame for fork/join persistence.
 */
public final class JoinFrame {

    private final String joinFrameId;
    private final Set<EntityId> expectedBranches;
    private final Map<EntityId, FrameToken> collectedTokens;

    public JoinFrame(String joinFrameId, Set<EntityId> expectedBranches) {
        this.joinFrameId = joinFrameId;
        this.expectedBranches = Set.copyOf(expectedBranches);
        this.collectedTokens = new LinkedHashMap<>();
    }

    public String joinFrameId() {
        return joinFrameId;
    }

    public Set<EntityId> expectedBranches() {
        return expectedBranches;
    }

    public synchronized void addToken(FrameToken token) {
        collectedTokens.put(token.branchNode(), token);
    }

    public synchronized boolean isComplete() {
        return collectedTokens.keySet().containsAll(expectedBranches);
    }

    public synchronized Set<EntityId> missingBranches() {
        Set<EntityId> missing = new LinkedHashSet<>(expectedBranches);
        missing.removeAll(collectedTokens.keySet());
        return Set.copyOf(missing);
    }

    public synchronized Map<EntityId, FrameToken> collectedTokens() {
        return Map.copyOf(collectedTokens);
    }

    public synchronized int collectedCount() {
        return collectedTokens.size();
    }

    public synchronized int expectedCount() {
        return expectedBranches.size();
    }
}
