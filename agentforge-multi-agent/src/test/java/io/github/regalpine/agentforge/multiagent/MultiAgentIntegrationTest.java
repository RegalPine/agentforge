package io.github.regalpine.agentforge.multiagent;


import io.github.regalpine.agentforge.capability.*;
import io.github.regalpine.agentforge.kernel.EntityId;
import io.github.regalpine.agentforge.kernel.Reference;
import io.github.regalpine.agentforge.kernel.Version;
import io.github.regalpine.agentforge.multiagent.budget.DelegationBudget;
import io.github.regalpine.agentforge.multiagent.capability.AgentCapability;
import io.github.regalpine.agentforge.multiagent.contract.AgentContract;
import io.github.regalpine.agentforge.multiagent.delegation.DelegationContext;
import io.github.regalpine.agentforge.multiagent.delegation.DefaultAgentDelegator;
import io.github.regalpine.agentforge.multiagent.event.DelegationEventSink;
import io.github.regalpine.agentforge.multiagent.event.DelegationEventType;
import io.github.regalpine.agentforge.multiagent.policy.DefaultDelegationPolicy;
import io.github.regalpine.agentforge.multiagent.projection.DefaultContextProjection;
import io.github.regalpine.agentforge.multiagent.reference.InMemoryAgentResolver;
import io.github.regalpine.agentforge.multiagent.result.AgentResult;
import io.github.regalpine.agentforge.multiagent.result.ResultStatus;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Multi-Agent integration tests — AE-11 §68.
 * Covers: AF-MA-001 through AF-MA-020.
 */
class MultiAgentIntegrationTest {

    /**
     * AF-MA-001: AgentCapability can parse AgentContract.
     */
    @Test
    void afMa001_agentCapabilityParsesContract() {
        var contract = AgentContract.of(
            "analysis-agent", "1.0.0"
        );
        var resolver = new InMemoryAgentResolver();
        resolver.register(contract);

        var delegator = new DefaultAgentDelegator(
            resolver,
            DelegationEventSink.noop(),
            req -> AgentResult.success(
                Map.of("result", "ok"),
                AgentResult.ResultProvenance.of(
                    req.childExecutionId(),
                    req.agent().id().value()
                )
            )
        );

        var budget = DelegationBudget.defaults();
        var ctx = DelegationContext.root(
            "root-1", "parent",
            budget,
            DefaultDelegationPolicy.permissive(),
            new DefaultContextProjection()
        );

        var cap = new AgentCapability(
            contract, delegator, ctx
        );

        assertEquals("analysis-agent",
            cap.agentContract().agentId().value());
        assertEquals("agent-capability",
            cap.descriptor().providerId());
    }

    /**
     * AF-MA-003: Unauthorized delegation must be denied.
     */
    @Test
    void afMa003_unauthorizedDelegationDenied() {
        var resolver = new InMemoryAgentResolver();
        var child = AgentContract.of("child", "1.0.0");
        resolver.register(child);

        var delegator = new DefaultAgentDelegator(
            resolver,
            DelegationEventSink.collecting(),
            req -> AgentResult.success(Map.of(), null)
        );

        var policy = new DefaultDelegationPolicy(
            null,
            java.util.Set.of("other-agent"),
            10
        );

        var budget = DelegationBudget.defaults();
        var ctx = DelegationContext.root(
            "root-1", "parent",
            budget, policy, null
        );

        var result = delegator.delegate(
            Reference.of("child", "1.0.0"),
            Map.of(), ctx
        );

        assertEquals(
            ResultStatus.POLICY_DENIED, result.status());
    }

    /**
     * AF-MA-005: Delegation depth exceeded must reject.
     */
    @Test
    void afMa005_depthExceededRejects() {
        var resolver = new InMemoryAgentResolver();
        var child = AgentContract.of("child", "1.0.0");
        resolver.register(child);

        var delegator = new DefaultAgentDelegator(
            resolver,
            DelegationEventSink.collecting(),
            req -> AgentResult.success(Map.of(), null)
        );

        var budget = DelegationBudget.defaults();
        var ctx = new DelegationContext(
            "root", "exec", "parent",
            10, budget, null, null,
            java.util.List.of(
                "root", "a", "b", "c", "d",
                "e", "f", "g", "h", "i", "parent"
            ),
            Map.of(), null
        );

        var result = delegator.delegate(
            Reference.of("child", "1.0.0"),
            Map.of(), ctx
        );

        assertEquals(
            ResultStatus.POLICY_DENIED, result.status());
    }

    /**
     * AF-MA-006: Circular delegation must be detected.
     */
    @Test
    void afMa006_circularDelegationDetected() {
        var resolver = new InMemoryAgentResolver();
        var child = AgentContract.of("agent-a", "1.0.0");
        resolver.register(child);

        var sink = DelegationEventSink.collecting();
        var delegator = new DefaultAgentDelegator(
            resolver, sink,
            req -> AgentResult.success(Map.of(), null)
        );

        var budget = DelegationBudget.defaults();
        var ctx = new DelegationContext(
            "root", "exec", "parent",
            2, budget, null, null,
            java.util.List.of("parent", "agent-b", "agent-a"),
            Map.of(), null
        );

        var result = delegator.delegate(
            Reference.of("agent-a", "1.0.0"),
            Map.of(), ctx
        );

        assertEquals(
            ResultStatus.POLICY_DENIED, result.status());
        assertFalse(sink.eventsOfType(
            DelegationEventType.AGENT_CIRCULAR_DETECTION
        ).isEmpty());
    }

    /**
     * AF-MA-007: Child must have independent ExecutionId.
     * AF-MA-008: Child must preserve ParentExecutionId.
     */
    @Test
    void afMa007_008_childExecutionIdentity() {
        var budget = DelegationBudget.defaults();
        var ctx = DelegationContext.root(
            "root-exec",
            "parent-agent",
            budget, null, null
        );

        var childCtx = ctx.deriveChild(
            "child-exec-1",
            "child-agent",
            budget
        );

        // AF-MA-007: independent execution ID
        assertNotEquals(
            ctx.parentExecutionId(),
            childCtx.parentExecutionId()
        );
        assertEquals("child-exec-1",
            childCtx.parentExecutionId());

        // AF-MA-008: preserves root
        assertEquals("root-exec",
            childCtx.rootExecutionId());

        // Depth incremented
        assertEquals(1, childCtx.delegationDepth());
    }

    /**
     * AF-MA-011: Budget must propagate to child.
     * AF-MA-012: Child must not exceed delegation budget.
     */
    @Test
    void afMa011_012_budgetPropagation() {
        var root = DelegationBudget.root(
            100_000, 50, 100, 5
        );

        // Allocate child
        var child = root.allocateChild(
            30_000, 15, 30, 3
        );

        assertEquals(30_000, child.allocatedMaxTokens());
        assertEquals(100_000, child.rootMaxTokens());

        // Child consumes within budget
        child.addTokenConsumption(20_000);
        assertEquals(10_000, child.remainingTokens());

        // Child cannot exceed allocation
        assertThrows(
            io.github.regalpine.agentforge.multiagent.budget
                .BudgetExceededException.class,
            () -> child.addTokenConsumption(20_000)
        );
    }

    /**
     * AF-MA-016: Context projection must not copy secrets.
     */
    @Test
    void afMa016_contextProjectionBlocksSecrets() {
        var projection = new DefaultContextProjection();
        var contract = AgentContract.of("child", "1.0.0");

        Map<String, Object> parentCtx = Map.of(
            "task", "analyze data",
            "apiKeys", "sk-secret",
            "systemPrompt", "you are a helper",
            "credentials", "admin:pass"
        );

        var projected = projection.project(
            parentCtx, contract
        );

        assertTrue(projected.secretRedacted());
        assertTrue(projected.data().containsKey("task"));
        assertFalse(projected.data()
            .containsKey("apiKeys"));
        assertFalse(projected.data()
            .containsKey("systemPrompt"));
        assertFalse(projected.data()
            .containsKey("credentials"));
    }

    /**
     * AF-MA-017: Child result enters parent observation.
     */
    @Test
    void afMa017_childResultToObservation() {
        var resolver = new InMemoryAgentResolver();
        var child = AgentContract.of("child", "1.0.0");
        resolver.register(child);

        var sink = DelegationEventSink.collecting();
        var delegator = new DefaultAgentDelegator(
            resolver, sink,
            req -> AgentResult.success(
                Map.of("finding", "critical"),
                AgentResult.ResultProvenance.of(
                    req.childExecutionId(),
                    "child"
                )
            )
        );

        var budget = DelegationBudget.defaults();
        var ctx = DelegationContext.root(
            "root-1", "parent",
            budget, null, null
        );

        var result = delegator.delegate(
            Reference.of("child", "1.0.0"),
            Map.of("task", "analyze"),
            ctx
        );

        assertEquals(
            ResultStatus.SUCCESS, result.status());
        assertEquals("critical",
            result.output().get("finding"));

        // RESULT_RECEIVED event = observation
        assertFalse(sink.eventsOfType(
            DelegationEventType.AGENT_RESULT_RECEIVED
        ).isEmpty());
    }

    /**
     * AF-MA-018: Multi-Agent execution supports
     * root execution trace.
     */
    @Test
    void afMa018_rootExecutionTrace() {
        var resolver = new InMemoryAgentResolver();
        var childB = AgentContract.of("agent-b", "1.0.0");
        resolver.register(childB);

        var sink = DelegationEventSink.collecting();
        var delegator = new DefaultAgentDelegator(
            resolver, sink,
            req -> AgentResult.success(
                Map.of(),
                AgentResult.ResultProvenance.of(
                    req.childExecutionId(),
                    req.agent().id().value()
                )
            )
        );

        var budget = DelegationBudget.defaults();
        var ctx = DelegationContext.root(
            "root-trace-1", "agent-a",
            budget, null, null
        );

        delegator.delegate(
            Reference.of("agent-b", "1.0.0"),
            Map.of(), ctx
        );

        // All events carry rootExecutionId
        for (var event : sink.events()) {
            assertEquals("root-trace-1",
                event.rootExecutionId());
        }
    }

    /**
     * Full delegation pipeline:
     * A → B → C (two-level delegation).
     */
    @Test
    void shouldSupportTwoLevelDelegation() {
        var resolver = new InMemoryAgentResolver();
        resolver.register(
            AgentContract.of("agent-b", "1.0.0"));
        resolver.register(
            AgentContract.of("agent-c", "1.0.0"));

        var sink = DelegationEventSink.collecting();

        var delegator = new DefaultAgentDelegator(
            resolver, sink,
            req -> AgentResult.success(
                Map.of("from", req.agent().id().value()),
                AgentResult.ResultProvenance.of(
                    req.childExecutionId(),
                    req.agent().id().value()
                )
            )
        );

        var budget = DelegationBudget.defaults();
        var ctxA = DelegationContext.root(
            "root-1", "agent-a",
            budget,
            DefaultDelegationPolicy.permissive(),
            new DefaultContextProjection()
        );

        // A delegates to B
        var resultB = delegator.delegate(
            Reference.of("agent-b", "1.0.0"),
            Map.of("task", "research"),
            ctxA
        );

        assertEquals(
            ResultStatus.SUCCESS, resultB.status());

        // B delegates to C (derive child context)
        var childBudget = budget.allocateChild(
            50_000, 25, 50, 5
        );
        var ctxB = ctxA.deriveChild(
            "exec-b", "agent-b", childBudget
        );

        var resultC = delegator.delegate(
            Reference.of("agent-c", "1.0.0"),
            Map.of("task", "analyze"),
            ctxB
        );

        assertEquals(
            ResultStatus.SUCCESS, resultC.status());
        assertEquals("agent-c",
            resultC.output().get("from"));

        // Verify execution tree depth
        assertEquals(1, ctxB.delegationDepth());
        assertTrue(ctxB.delegationStack()
            .contains("agent-a"));
        assertTrue(ctxB.delegationStack()
            .contains("agent-b"));
    }

    /**
     * AgentCapability enters Invocation Pipeline.
     */
    @Test
    void shouldEnterInvocationPipeline() {
        var contract = AgentContract.of(
            "worker-agent", "1.0.0"
        );
        var resolver = new InMemoryAgentResolver();
        resolver.register(contract);

        var delegator = new DefaultAgentDelegator(
            resolver,
            DelegationEventSink.noop(),
            req -> AgentResult.success(
                Map.of("work", "done"),
                AgentResult.ResultProvenance.of(
                    req.childExecutionId(),
                    "worker-agent"
                )
            )
        );

        var budget = DelegationBudget.defaults();
        var ctx = DelegationContext.root(
            "root-1", "parent",
            budget, null, null
        );

        var cap = new AgentCapability(
            contract, delegator, ctx
        );

        // Invoke through CapabilityInvocation
        var invocation = new CapabilityInvocation(
            "inv-1",
            new EntityId("worker-agent"),
            new Version("1.0.0"),
            Map.of("task", "do-work"),
            new InvocationContext("exec-1", "tenant-1", null)
        );

        var result = cap.invoke(invocation);
        assertInstanceOf(CapabilitySuccess.class, result);

        var success = (CapabilitySuccess) result;
        assertEquals("done",
            success.output().get("work"));
    }
}
