package io.github.regalpine.agentforge.multiagent;


import io.github.regalpine.agentforge.multiagent.handoff.HandoffPolicy;
import io.github.regalpine.agentforge.multiagent.handoff.HandoffRequest;
import io.github.regalpine.agentforge.multiagent.handoff.HandoffResult;
import io.github.regalpine.agentforge.multiagent.invocation.AgentInvocation;
import io.github.regalpine.agentforge.multiagent.invocation.InvocationId;
import io.github.regalpine.agentforge.multiagent.invocation.InvocationMode;
import io.github.regalpine.agentforge.multiagent.invocation.InvocationStatus;
import io.github.regalpine.agentforge.multiagent.messages.AgentMessage;
import io.github.regalpine.agentforge.multiagent.messages.MessageDelivery;
import io.github.regalpine.agentforge.multiagent.messages.MessageId;
import io.github.regalpine.agentforge.multiagent.reference.AgentReference;
import io.github.regalpine.agentforge.multiagent.reference.AgentTask;
import io.github.regalpine.agentforge.multiagent.registry.AgentQuery;
import io.github.regalpine.agentforge.multiagent.registry.AgentRegistry;
import io.github.regalpine.agentforge.multiagent.registry.InMemoryAgentRegistry;
import io.github.regalpine.agentforge.multiagent.shared.SharedContext;
import io.github.regalpine.agentforge.multiagent.shared.SharedWorkspace;
import io.github.regalpine.agentforge.multiagent.team.AgentTeam;
import io.github.regalpine.agentforge.multiagent.team.CollaborationTopology;
import io.github.regalpine.agentforge.multiagent.team.CompletionCriteria;
import io.github.regalpine.agentforge.multiagent.team.TeamExecution;
import io.github.regalpine.agentforge.multiagent.team.TeamExecutionStatus;
import io.github.regalpine.agentforge.multiagent.team.TeamId;
import io.github.regalpine.agentforge.multiagent.team.TeamPolicy;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Multi-Agent Runtime tests.
 * AF-ENHANCE-08 §79: TCK.
 *
 * <p>Covers: Invocation, Handoff,
 * Team, Message, SharedContext,
 * SharedWorkspace, Registry,
 * Reference, Task.
 */
class MultiAgentRuntimeTest {

    // ====================
    // InvocationId
    // ====================

    @Test
    void invocationId_generate() {
        var id = InvocationId.generate();
        assertNotNull(id.value());
        assertFalse(id.value().isBlank());
    }

    @Test
    void invocationId_of() {
        var id = InvocationId.of("inv-001");
        assertEquals("inv-001", id.value());
        assertEquals("inv-001", id.toString());
    }

    @Test
    void invocationId_nullThrows() {
        assertThrows(NullPointerException.class,
            () -> new InvocationId(null));
    }

    @Test
    void invocationId_blankThrows() {
        assertThrows(
            IllegalArgumentException.class,
            () -> new InvocationId("  ")
        );
    }

    // ====================
    // InvocationMode
    // ====================

    @Test
    void invocationMode_values() {
        var modes = InvocationMode.values();
        assertEquals(3, modes.length);
        assertNotNull(InvocationMode.DELEGATE);
        assertNotNull(InvocationMode.HANDOFF);
        assertNotNull(InvocationMode.COLLABORATE);
    }

    // ====================
    // InvocationStatus
    // ====================

    @Test
    void invocationStatus_terminal() {
        assertTrue(
            InvocationStatus.COMPLETED.isTerminal()
        );
        assertTrue(
            InvocationStatus.REJECTED.isTerminal()
        );
        assertTrue(
            InvocationStatus.FAILED.isTerminal()
        );
        assertTrue(
            InvocationStatus.CANCELLED.isTerminal()
        );
        assertTrue(
            InvocationStatus.TIMEOUT.isTerminal()
        );
        assertFalse(
            InvocationStatus.REQUESTED.isTerminal()
        );
        assertFalse(
            InvocationStatus.RUNNING.isTerminal()
        );
    }

    @Test
    void invocationStatus_active() {
        assertTrue(
            InvocationStatus.STARTED.isActive()
        );
        assertTrue(
            InvocationStatus.RUNNING.isActive()
        );
        assertFalse(
            InvocationStatus.COMPLETED.isActive()
        );
    }

    @Test
    void invocationStatus_transitions() {
        assertTrue(
            InvocationStatus.REQUESTED
                .canTransitionTo(InvocationStatus.AUTHORIZED)
        );
        assertTrue(
            InvocationStatus.REQUESTED
                .canTransitionTo(InvocationStatus.REJECTED)
        );
        assertFalse(
            InvocationStatus.REQUESTED
                .canTransitionTo(InvocationStatus.COMPLETED)
        );
        assertTrue(
            InvocationStatus.RUNNING
                .canTransitionTo(InvocationStatus.COMPLETED)
        );
        assertTrue(
            InvocationStatus.RUNNING
                .canTransitionTo(InvocationStatus.FAILED)
        );
        assertFalse(
            InvocationStatus.COMPLETED
                .canTransitionTo(InvocationStatus.RUNNING)
        );
    }

    // ====================
    // AgentInvocation
    // ====================

    @Test
    void agentInvocation_create() {
        var inv = AgentInvocation.create(
            "agent-a", "agent-b",
            "exec-a",
            InvocationMode.DELEGATE
        );

        assertNotNull(inv.id());
        assertEquals("agent-a", inv.callerAgentId());
        assertEquals("agent-b", inv.targetAgentId());
        assertEquals("exec-a",
            inv.callerExecutionId());
        assertNull(inv.targetExecutionId());
        assertEquals(InvocationMode.DELEGATE,
            inv.mode());
        assertEquals(InvocationStatus.REQUESTED,
            inv.status());
        assertTrue(inv.isDelegation());
        assertFalse(inv.isHandoff());
        assertFalse(inv.isCollaboration());
    }

    @Test
    void agentInvocation_statusTransition() {
        var inv = AgentInvocation.create(
            "a", "b", "e1",
            InvocationMode.HANDOFF
        );

        var authorized = inv.withStatus(
            InvocationStatus.AUTHORIZED
        );
        assertEquals(
            InvocationStatus.AUTHORIZED,
            authorized.status()
        );

        var started = authorized.withStatus(
            InvocationStatus.STARTED
        );
        assertEquals(
            InvocationStatus.STARTED,
            started.status()
        );

        var running = started.withStatus(
            InvocationStatus.RUNNING
        );
        var completed = running.withStatus(
            InvocationStatus.COMPLETED
        );
        assertTrue(completed.isComplete());
        assertTrue(completed.isHandoff());
    }

    @Test
    void agentInvocation_invalidTransition() {
        var inv = AgentInvocation.create(
            "a", "b", "e1",
            InvocationMode.DELEGATE
        );

        assertThrows(
            IllegalStateException.class,
            () -> inv.withStatus(
                InvocationStatus.COMPLETED
            )
        );
    }

    @Test
    void agentInvocation_withTargetExecution() {
        var inv = AgentInvocation.create(
            "a", "b", "e1",
            InvocationMode.DELEGATE
        );
        var withTarget = inv.withTargetExecution(
            "e2"
        );
        assertEquals("e2",
            withTarget.targetExecutionId());
    }

    @Test
    void agentInvocation_withParent() {
        var inv = AgentInvocation.create(
            "a", "b", "e1",
            InvocationMode.DELEGATE
        );
        var withParent = inv.withParent("p1");
        assertEquals("p1",
            withParent.parentInvocationId());
    }

    // ====================
    // HandoffPolicy
    // ====================

    @Test
    void handoffPolicy_values() {
        assertEquals(3,
            HandoffPolicy.values().length);
        assertNotNull(HandoffPolicy.WAIT);
        assertNotNull(HandoffPolicy.TERMINATE);
        assertNotNull(HandoffPolicy.SUSPEND);
    }

    // ====================
    // HandoffRequest
    // ====================

    @Test
    void handoffRequest_simple() {
        var req = HandoffRequest.of("task1");
        assertEquals("task1", req.task());
        assertTrue(req.contextItems().isEmpty());
        assertTrue(req.artifactRefs().isEmpty());
        assertEquals(HandoffPolicy.WAIT,
            req.policy());
    }

    @Test
    void handoffRequest_withContext() {
        var req = HandoffRequest.of(
            "task1",
            Map.of("key", "value")
        );
        assertEquals("value",
            req.contextItems().get("key"));
    }

    @Test
    void handoffRequest_builder() {
        var req = HandoffRequest.builder()
            .task("analyze")
            .context(Map.of("data", "x"))
            .artifacts(List.of("art-1"))
            .policy(HandoffPolicy.TERMINATE)
            .build();

        assertEquals("analyze", req.task());
        assertEquals(HandoffPolicy.TERMINATE,
            req.policy());
        assertEquals(1,
            req.artifactRefs().size());
    }

    @Test
    void handoffRequest_nullTaskThrows() {
        assertThrows(NullPointerException.class,
            () -> HandoffRequest.of(null));
    }

    // ====================
    // HandoffResult
    // ====================

    @Test
    void handoffResult_success() {
        var result = HandoffResult.success(
            "h1", "a", "b", "e2",
            HandoffPolicy.WAIT
        );
        assertEquals(
            HandoffResult.HandoffStatus.COMPLETED,
            result.status()
        );
        assertEquals("e2",
            result.targetExecutionId());
    }

    @Test
    void handoffResult_rejected() {
        var result = HandoffResult.rejected(
            "h1", "a", "b",
            "Policy denied"
        );
        assertEquals(
            HandoffResult.HandoffStatus.REJECTED,
            result.status()
        );
        assertEquals("Policy denied",
            result.metadata().get("reason"));
    }

    // ====================
    // TeamId
    // ====================

    @Test
    void teamId_generate() {
        var id = TeamId.generate();
        assertNotNull(id.value());
    }

    @Test
    void teamId_of() {
        var id = TeamId.of("team-1");
        assertEquals("team-1", id.value());
    }

    // ====================
    // CollaborationTopology
    // ====================

    @Test
    void topology_values() {
        assertEquals(6,
            CollaborationTopology.values().length);
        assertNotNull(
            CollaborationTopology.STAR);
        assertNotNull(
            CollaborationTopology.PEER);
    }

    // ====================
    // CompletionCriteria
    // ====================

    @Test
    void completionCriteria_values() {
        assertEquals(4,
            CompletionCriteria.values().length);
    }

    // ====================
    // TeamPolicy
    // ====================

    @Test
    void teamPolicy_defaults() {
        var p = TeamPolicy.defaults();
        assertEquals(CompletionCriteria.ALL,
            p.completion());
        assertEquals(4, p.maxConcurrency());
        assertTrue(p.propagateCancellation());
    }

    @Test
    void teamPolicy_permissive() {
        var p = TeamPolicy.permissive();
        assertEquals(CompletionCriteria.ANY,
            p.completion());
        assertEquals(16, p.maxConcurrency());
    }

    @Test
    void teamPolicy_requiredOptional() {
        var p = new TeamPolicy(
            CompletionCriteria.ALL,
            2, 4,
            List.of("a", "b"),
            List.of("c"),
            0, 0, true
        );
        assertTrue(p.isRequired("a"));
        assertFalse(p.isRequired("c"));
        assertTrue(p.isOptional("c"));
    }

    // ====================
    // AgentTeam
    // ====================

    @Test
    void agentTeam_create() {
        var team = AgentTeam.create(
            "research",
            List.of("a", "b", "c"),
            CollaborationTopology.STAR
        );
        assertNotNull(team.id());
        assertEquals("research", team.name());
        assertEquals(3, team.memberCount());
        assertTrue(team.hasMember("a"));
        assertFalse(team.hasMember("z"));
    }

    @Test
    void agentTeam_emptyMembersThrows() {
        assertThrows(
            IllegalArgumentException.class,
            () -> AgentTeam.create(
                "empty", List.of(),
                CollaborationTopology.STAR
            )
        );
    }

    // ====================
    // TeamExecutionStatus
    // ====================

    @Test
    void teamExecutionStatus_terminal() {
        assertTrue(
            TeamExecutionStatus.COMPLETED
                .isTerminal()
        );
        assertTrue(
            TeamExecutionStatus.FAILED
                .isTerminal()
        );
        assertFalse(
            TeamExecutionStatus.RUNNING
                .isTerminal()
        );
    }

    // ====================
    // TeamExecution
    // ====================

    @Test
    void teamExecution_lifecycle() {
        var teamId = TeamId.of("t1");
        var exec = TeamExecution.create(
            teamId, "parent-e1"
        );

        assertEquals(
            TeamExecutionStatus.CREATED,
            exec.status()
        );

        var planned = exec.withStatus(
            TeamExecutionStatus.PLANNED
        );
        assertEquals(
            TeamExecutionStatus.PLANNED,
            planned.status()
        );

        var running = planned.withStatus(
            TeamExecutionStatus.RUNNING
        );
        assertTrue(running.status().isActive());

        var withMember = running.addMember("m1");
        assertEquals(1,
            withMember.memberExecutionIds().size());

        var completed = withMember.withStatus(
            TeamExecutionStatus.COMPLETED
        );
        assertTrue(completed.isComplete());
        assertNotNull(completed.completedAt());
    }

    // ====================
    // MessageId
    // ====================

    @Test
    void messageId_generate() {
        var id = MessageId.generate();
        assertNotNull(id.value());
    }

    // ====================
    // MessageDelivery
    // ====================

    @Test
    void messageDelivery_values() {
        assertEquals(2,
            MessageDelivery.values().length);
    }

    // ====================
    // AgentMessage
    // ====================

    @Test
    void agentMessage_create() {
        var msg = AgentMessage.of(
            "a", "b", "request",
            "analyze this"
        );
        assertNotNull(msg.id());
        assertEquals("a", msg.senderAgentId());
        assertEquals("b", msg.receiverAgentId());
        assertEquals("request", msg.type());
        assertEquals(
            MessageDelivery.AT_LEAST_ONCE,
            msg.delivery()
        );
    }

    @Test
    void agentMessage_withCorrelation() {
        var msg = AgentMessage.of(
            "a", "b", "req", "data",
            "corr-1"
        );
        assertEquals("corr-1",
            msg.correlationId());
    }

    @Test
    void agentMessage_reply() {
        var msg = AgentMessage.of(
            "a", "b", "req", "data"
        );
        var reply = msg.reply(
            "response", "result"
        );
        assertEquals("b", reply.senderAgentId());
        assertEquals("a", reply.receiverAgentId());
        assertEquals("response", reply.type());
    }

    // ====================
    // SharedContext
    // ====================

    @Test
    void sharedContext_create() {
        var ctx = SharedContext.create(
            "agent-a",
            Set.of("agent-b"),
            Map.of("data", "value")
        );
        assertEquals("agent-a",
            ctx.sourceAgentId());
        assertTrue(
            ctx.isAccessibleBy("agent-b")
        );
        assertFalse(
            ctx.isAccessibleBy("agent-c")
        );
        assertFalse(ctx.isExpired());
    }

    @Test
    void sharedContext_emptyTargets() {
        var ctx = SharedContext.create(
            "a", null,
            Map.of("k", "v")
        );
        // Empty targets = accessible to all
        assertTrue(
            ctx.isAccessibleBy("anyone")
        );
    }

    @Test
    void sharedContext_expired() {
        var ctx = new SharedContext(
            "c1", "a", Set.of("b"),
            Map.of(), null, null, null,
            Instant.now().minus(1, ChronoUnit.HOURS),
            null
        );
        assertTrue(ctx.isExpired());
    }

    // ====================
    // SharedWorkspace
    // ====================

    @Test
    void sharedWorkspace_create() {
        var ws = SharedWorkspace.create(
            "ws-1",
            Set.of("a", "b")
        );
        assertEquals(
            SharedWorkspace.ConflictResolution.VERSION,
            ws.conflictResolution()
        );
        assertTrue(ws.isAccessibleBy("a"));
        assertTrue(ws.isAccessibleBy("b"));
        assertFalse(ws.isAccessibleBy("c"));
    }

    @Test
    void sharedWorkspace_emptyParticipants() {
        var ws = SharedWorkspace.create(
            "ws-1", null
        );
        assertTrue(ws.isAccessibleBy("anyone"));
    }

    // ====================
    // AgentRegistry
    // ====================

    @Test
    void agentRegistry_registerAndGet() {
        var reg = new InMemoryAgentRegistry();

        var def = AgentRegistry.AgentDefinition.of(
            "analyst", "Data Analyst"
        );
        reg.register(def);

        var found = reg.get("analyst");
        assertTrue(found.isPresent());
        assertEquals("Data Analyst",
            found.get().name());
    }

    @Test
    void agentRegistry_findByCapability() {
        var reg = new InMemoryAgentRegistry();

        reg.register(
            new AgentRegistry.AgentDefinition(
                "a1", "Agent 1", null, null,
                Set.of("analysis", "ml"),
                null, null, null
            )
        );
        reg.register(
            new AgentRegistry.AgentDefinition(
                "a2", "Agent 2", null, null,
                Set.of("research"),
                null, null, null
            )
        );

        var results = reg.find(
            AgentQuery.byCapability("analysis")
        );
        assertEquals(1, results.size());
        assertEquals("a1",
            results.get(0).agentId());
    }

    @Test
    void agentRegistry_findBySkill() {
        var reg = new InMemoryAgentRegistry();

        reg.register(
            new AgentRegistry.AgentDefinition(
                "a1", "Agent 1", null, null,
                null, Set.of("code-review"),
                null, null
            )
        );

        var results = reg.find(
            AgentQuery.bySkill("code-review")
        );
        assertEquals(1, results.size());
    }

    @Test
    void agentRegistry_unregister() {
        var reg = new InMemoryAgentRegistry();
        reg.register(
            AgentRegistry.AgentDefinition.of(
                "a1", "Agent 1"
            )
        );
        assertEquals(1, reg.size());

        assertTrue(reg.unregister("a1"));
        assertEquals(0, reg.size());
        assertFalse(reg.get("a1").isPresent());
    }

    @Test
    void agentRegistry_listAll() {
        var reg = new InMemoryAgentRegistry();
        reg.register(
            AgentRegistry.AgentDefinition.of(
                "a1", "A1"
            )
        );
        reg.register(
            AgentRegistry.AgentDefinition.of(
                "a2", "A2"
            )
        );
        assertEquals(2, reg.listAll().size());
    }

    // ====================
    // AgentReference
    // ====================

    @Test
    void agentReference_of() {
        var ref = AgentReference.of(
            "analyst", "1.2.0"
        );
        assertEquals("analyst", ref.agentId());
        assertEquals("1.2.0", ref.version());
        assertNull(ref.digest());
    }

    @Test
    void agentReference_withDigest() {
        var ref = AgentReference.of(
            "a", "1.0", "sha256:abc"
        );
        assertEquals("sha256:abc", ref.digest());
    }

    @Test
    void agentReference_defaultVersion() {
        var ref = AgentReference.of("a");
        assertEquals("1.0.0", ref.version());
    }

    // ====================
    // AgentTask
    // ====================

    @Test
    void agentTask_simple() {
        var task = AgentTask.of("analyze");
        assertEquals("analyze",
            task.description());
        assertTrue(task.inputData().isEmpty());
    }

    @Test
    void agentTask_withInput() {
        var task = AgentTask.of(
            "analyze",
            Map.of("data", "x")
        );
        assertEquals("x",
            task.inputData().get("data"));
    }

    @Test
    void agentTask_full() {
        var task = AgentTask.of(
            "t1", "desc",
            Map.of("k", "v"),
            List.of("output1"),
            Map.of("maxTime", "60s")
        );
        assertEquals("t1", task.taskId());
        assertEquals(1,
            task.requiredOutputs().size());
    }
}
