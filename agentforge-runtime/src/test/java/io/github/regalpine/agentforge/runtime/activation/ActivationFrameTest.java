package io.github.regalpine.agentforge.runtime.activation;

import io.github.regalpine.agentforge.kernel.EntityId;
import io.github.regalpine.agentforge.event.ExecutionId;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for G-01: Activation/Token/Frame persistence.
 */
class ActivationFrameTest {

    @Test
    void shouldTrackJoinFrameCompletion() {
        EntityId branch1 = new EntityId("branch-1");
        EntityId branch2 = new EntityId("branch-2");
        EntityId branch3 = new EntityId("branch-3");

        JoinFrame frame = new JoinFrame("join-1", Set.of(branch1, branch2, branch3));
        assertFalse(frame.isComplete());
        assertEquals(0, frame.collectedCount());
        assertEquals(3, frame.expectedCount());

        // Add first token
        frame.addToken(new FrameToken("t1", new ExecutionId("exec-1"), "join-1",
                branch1, Map.of("result", "a"), Instant.now()));
        assertFalse(frame.isComplete());
        assertEquals(1, frame.collectedCount());

        // Add second token
        frame.addToken(new FrameToken("t2", new ExecutionId("exec-1"), "join-1",
                branch2, Map.of("result", "b"), Instant.now()));
        assertFalse(frame.isComplete());
        assertEquals(2, frame.collectedCount());
        assertEquals(Set.of(branch3), frame.missingBranches());

        // Add third token - should complete
        frame.addToken(new FrameToken("t3", new ExecutionId("exec-1"), "join-1",
                branch3, Map.of("result", "c"), Instant.now()));
        assertTrue(frame.isComplete());
        assertEquals(3, frame.collectedCount());
        assertTrue(frame.missingBranches().isEmpty());
    }

    @Test
    void shouldStoreAndRetrieveActivation() {
        InMemoryActivationStore store = new InMemoryActivationStore();
        ExecutionId execId = new ExecutionId("exec-1");
        EntityId sourceNode = new EntityId("parallel-1");

        Activation activation = new Activation(
                "act-1", execId, sourceNode, ActivationStatus.ACTIVE,
                Set.of(new EntityId("b1"), new EntityId("b2")),
                Map.of("fork", "true"), Instant.now()
        );
        store.saveActivation(activation);

        var found = store.findActivation("act-1");
        assertTrue(found.isPresent());
        assertEquals(ActivationStatus.ACTIVE, found.get().status());
        assertEquals(2, found.get().branches().size());

        var byExec = store.listByExecution(execId);
        assertEquals(1, byExec.size());
    }

    @Test
    void shouldStoreAndRetrieveTokens() {
        InMemoryActivationStore store = new InMemoryActivationStore();
        ExecutionId execId = new ExecutionId("exec-1");

        FrameToken t1 = new FrameToken("t1", execId, "jf-1",
                new EntityId("b1"), Map.of("v", 1), Instant.now());
        FrameToken t2 = new FrameToken("t2", execId, "jf-1",
                new EntityId("b2"), Map.of("v", 2), Instant.now());

        store.saveToken(t1);
        store.saveToken(t2);

        var tokens = store.listTokensByFrame("jf-1");
        assertEquals(2, tokens.size());
    }
}
