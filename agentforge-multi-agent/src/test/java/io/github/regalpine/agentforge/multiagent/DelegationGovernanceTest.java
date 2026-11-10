package io.github.regalpine.agentforge.multiagent;

import io.github.regalpine.agentforge.multiagent.budget.BudgetExceededException;
import io.github.regalpine.agentforge.multiagent.budget.DelegationBudget;
import io.github.regalpine.agentforge.multiagent.policy.DefaultDelegationPolicy;
import io.github.regalpine.agentforge.multiagent.policy.DelegationPolicyDecision;
import io.github.regalpine.agentforge.multiagent.projection.DefaultContextProjection;
import io.github.regalpine.agentforge.multiagent.projection.ContextProjection;
import io.github.regalpine.agentforge.multiagent.contract.AgentContract;
import io.github.regalpine.agentforge.kernel.Reference;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Budget, Policy, Projection tests —
 * AE-11 §7-8, §18-22.
 * AF-MA-004, AF-MA-011, AF-MA-012, AF-MA-016.
 */
class DelegationGovernanceTest {

    // --- Budget ---

    @Test
    void shouldCreateRootBudget() {
        var budget = DelegationBudget.root(
            100_000, 50, 100, 10
        );
        assertEquals(100_000, budget.allocatedMaxTokens());
        assertEquals(100_000, budget.remainingTokens());
        assertEquals(0, budget.consumedTokens());
        assertFalse(budget.isExhausted());
    }

    @Test
    void shouldConsumeTokens() {
        var budget = DelegationBudget.root(
            1000, 10, 10, 5
        );
        budget.addTokenConsumption(500);
        assertEquals(500, budget.remainingTokens());
        assertEquals(500, budget.consumedTokens());
    }

    @Test
    void shouldRejectOverConsumption() {
        var budget = DelegationBudget.root(
            1000, 10, 10, 5
        );
        assertThrows(
            BudgetExceededException.class,
            () -> budget.addTokenConsumption(1001)
        );
    }

    @Test
    void shouldAllocateChildBudget() {
        var parent = DelegationBudget.root(
            100_000, 50, 100, 10
        );
        var child = parent.allocateChild(
            50_000, 25, 50, 5
        );
        assertEquals(50_000, child.allocatedMaxTokens());
        assertEquals(50_000, child.remainingTokens());
        assertEquals(0, child.consumedTokens());
    }

    @Test
    void shouldRejectChildAllocationExceedingParent() {
        var parent = DelegationBudget.root(
            1000, 10, 10, 5
        );
        parent.addTokenConsumption(800);
        // Only 200 remaining, try to allocate 500
        assertThrows(
            BudgetExceededException.class,
            () -> parent.allocateChild(500, 5, 5, 2)
        );
    }

    @Test
    void shouldTrackModelCalls() {
        var budget = DelegationBudget.root(
            100_000, 5, 100, 10
        );
        budget.addModelCallConsumption();
        budget.addModelCallConsumption();
        assertEquals(3, budget.remainingModelCalls());
    }

    @Test
    void shouldRejectModelCallOverBudget() {
        var budget = DelegationBudget.root(
            100_000, 2, 100, 10
        );
        budget.addModelCallConsumption();
        budget.addModelCallConsumption();
        assertThrows(
            BudgetExceededException.class,
            budget::addModelCallConsumption
        );
    }

    @Test
    void shouldTrackToolCalls() {
        var budget = DelegationBudget.root(
            100_000, 50, 3, 10
        );
        budget.addToolCallConsumption();
        budget.addToolCallConsumption();
        budget.addToolCallConsumption();
        assertThrows(
            BudgetExceededException.class,
            budget::addToolCallConsumption
        );
    }

    @Test
    void shouldTrackChildExecutions() {
        var budget = DelegationBudget.root(
            100_000, 50, 100, 2
        );
        budget.addChildExecutionConsumption();
        budget.addChildExecutionConsumption();
        assertThrows(
            BudgetExceededException.class,
            budget::addChildExecutionConsumption
        );
    }

    @Test
    void shouldDetectExhaustedBudget() {
        var budget = DelegationBudget.root(
            100, 1, 1, 1
        );
        budget.addTokenConsumption(100);
        assertTrue(budget.isExhausted());
    }

    // --- Policy ---

    @Test
    void shouldAllowWithPermissivePolicy() {
        var policy = DefaultDelegationPolicy.permissive();
        var parent = Reference.of("agent-a", "1.0.0");
        var child = Reference.of("agent-b", "1.0.0");

        assertEquals(
            DelegationPolicyDecision.ALLOW,
            policy.authorize(parent, child, Map.of())
        );
    }

    @Test
    void shouldDenyDeniedAgent() {
        var policy = new DefaultDelegationPolicy(
            Set.of("evil-agent"),
            Set.of(), 10
        );
        var parent = Reference.of("agent-a", "1.0.0");
        var child = Reference.of("evil-agent", "1.0.0");

        assertEquals(
            DelegationPolicyDecision.DENY,
            policy.authorize(parent, child, Map.of())
        );
    }

    @Test
    void shouldDenyNonAllowedAgent() {
        var policy = DefaultDelegationPolicy.restrictive(
            Set.of("allowed-agent"), 10
        );
        var parent = Reference.of("a", "1.0.0");
        var child = Reference.of("other", "1.0.0");

        assertEquals(
            DelegationPolicyDecision.DENY,
            policy.authorize(parent, child, Map.of())
        );
    }

    @Test
    void shouldAllowExplicitAllowedAgent() {
        var policy = DefaultDelegationPolicy.restrictive(
            Set.of("good-agent"), 10
        );
        var parent = Reference.of("a", "1.0.0");
        var child = Reference.of("good-agent", "1.0.0");

        assertEquals(
            DelegationPolicyDecision.ALLOW,
            policy.authorize(parent, child, Map.of())
        );
    }

    @Test
    void shouldDenyWhenDepthExceeded() {
        var policy = DefaultDelegationPolicy.restrictive(
            Set.of(), 3
        );
        var parent = Reference.of("a", "1.0.0");
        var child = Reference.of("b", "1.0.0");

        assertEquals(
            DelegationPolicyDecision.DENY,
            policy.authorize(
                parent, child,
                Map.of("currentDepth", 3)
            )
        );
    }

    // --- Context Projection ---

    @Test
    void shouldProjectAllowedKeys() {
        var projection = new DefaultContextProjection();
        var contract = AgentContract.of("child", "1.0.0");

        Map<String, Object> parentCtx = Map.of(
            "task", "analyze",
            "userInput", "data",
            "tenantId", "t1"
        );

        var projected = projection.project(
            parentCtx, contract
        );

        assertEquals("analyze", projected.data().get("task"));
        assertEquals("data", projected.data().get("userInput"));
        assertEquals("t1", projected.data().get("tenantId"));
        assertFalse(projected.secretRedacted());
    }

    @Test
    void shouldBlockSecrets() {
        var projection = new DefaultContextProjection();
        var contract = AgentContract.of("child", "1.0.0");

        Map<String, Object> parentCtx = Map.of(
            "task", "analyze",
            "apiKeys", "secret-key",
            "credentials", "user:pass",
            "systemPrompt", "you are..."
        );

        var projected = projection.project(
            parentCtx, contract
        );

        assertEquals(1, projected.data().size());
        assertEquals("analyze", projected.data().get("task"));
        assertTrue(projected.secretRedacted());
        assertFalse(projected.data().containsKey("apiKeys"));
        assertFalse(projected.data().containsKey("credentials"));
        assertFalse(projected.data().containsKey("systemPrompt"));
    }

    @Test
    void shouldHandleEmptyContext() {
        var projection = new DefaultContextProjection();
        var contract = AgentContract.of("child", "1.0.0");

        var projected = projection.project(
            Map.of(), contract
        );

        assertTrue(projected.data().isEmpty());
        assertFalse(projected.secretRedacted());
    }

    @Test
    void shouldHandleNullContext() {
        var projection = new DefaultContextProjection();
        var contract = AgentContract.of("child", "1.0.0");

        var projected = projection.project(
            null, contract
        );

        assertTrue(projected.data().isEmpty());
    }
}
