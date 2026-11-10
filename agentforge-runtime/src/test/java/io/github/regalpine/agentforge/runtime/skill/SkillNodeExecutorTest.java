package io.github.regalpine.agentforge.runtime.skill;

import io.github.regalpine.agentforge.event.Execution;
import io.github.regalpine.agentforge.event.ExecutionState;
import io.github.regalpine.agentforge.event.ExecutionStatus;
import io.github.regalpine.agentforge.event.ExecutionId;

import io.github.regalpine.agentforge.graph.NodeType;
import io.github.regalpine.agentforge.graph.ir.ExecutionNode;
import io.github.regalpine.agentforge.kernel.*;
import io.github.regalpine.agentforge.skill.governance.*;
import io.github.regalpine.agentforge.runtime.*;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

import static org.junit.jupiter.api.Assertions.*;

/**
 * SkillNodeExecutor 测试.
 * T-S2.20: SkillNode 执行 /
 * StateDelta / 不创建独立 Engine.
 */
class SkillNodeExecutorTest {

    @Test
    void shouldRejectNonSkillNodeType() {
        SkillInvoker stub = req ->
            CompletableFuture.completedFuture(
                SkillResult.success(
                    req.skill(), Map.of()
                )
            );

        SkillNodeExecutor executor =
            new SkillNodeExecutor(stub);

        ExecutionNode node = new ExecutionNode(
            new EntityId("n1"),
            NodeType.CAPABILITY,
            Map.of("skillId", "s1"),
            Map.of()
        );

        Execution exec = new Execution(
            ExecutionId.generate(),
            new EntityId("g1"),
            new Version("1.0.0"),
            "fp",
            ExecutionStatus.RUNNING,
            Instant.now(),
            null, null
        );

        ExecutionState state = new ExecutionState(
            exec.id(),
            ExecutionStatus.RUNNING,
            new EntityId("n1"),
            Map.of(),
            java.util.Set.of(),
            java.util.Set.of(),
            0
        );

        NodeExecutionContext ctx =
            new NodeExecutionContext(
                exec, state, null, Map.of()
            );

        NodeExecutionResult result =
            executor.execute(node, ctx);

        assertInstanceOf(
            NodeFailed.class, result
        );
        assertEquals(
            "SKILL_TYPE_MISMATCH",
            ((NodeFailed) result).code()
        );
    }

    @Test
    void shouldReturnCompletedOnSuccess()
        throws Exception
    {
        SkillReference ref =
            SkillReference.of("s1", "1.0.0");

        SkillInvoker stub = req ->
            CompletableFuture.completedFuture(
                SkillResult.success(
                    ref,
                    Map.of("result", "done")
                )
            );

        SkillNodeExecutor executor =
            new SkillNodeExecutor(stub);

        ExecutionNode node = new ExecutionNode(
            new EntityId("n1"),
            NodeType.SKILL,
            Map.of(
                "skillId", "s1",
                "skillVersion", "1.0.0"
            ),
            Map.of()
        );

        Execution exec = new Execution(
            ExecutionId.generate(),
            new EntityId("g1"),
            new Version("1.0.0"),
            "fp",
            ExecutionStatus.RUNNING,
            Instant.now(),
            null, null
        );

        ExecutionState state = new ExecutionState(
            exec.id(),
            ExecutionStatus.RUNNING,
            new EntityId("n1"),
            Map.of(),
            java.util.Set.of(),
            java.util.Set.of(),
            0
        );

        NodeExecutionContext ctx =
            new NodeExecutionContext(
                exec, state, null, Map.of()
        );

        NodeExecutionResult result =
            executor.execute(node, ctx);

        assertInstanceOf(
            NodeCompleted.class, result
        );
        assertEquals("done",
            ((NodeCompleted) result)
                .output().get("result")
        );
    }

    @Test
    void shouldReturnFailedOnSkillError()
        throws Exception
    {
        SkillReference ref =
            SkillReference.of("s1", "1.0.0");

        SkillInvoker stub = req ->
            CompletableFuture.completedFuture(
                SkillResult.failed(
                    ref,
                    SkillErrorCode.SKILL_TIMEOUT,
                    "timed out"
                )
            );

        SkillNodeExecutor executor =
            new SkillNodeExecutor(stub);

        ExecutionNode node = new ExecutionNode(
            new EntityId("n1"),
            NodeType.SKILL,
            Map.of("skillId", "s1"),
            Map.of()
        );

        Execution exec = new Execution(
            ExecutionId.generate(),
            new EntityId("g1"),
            new Version("1.0.0"),
            "fp",
            ExecutionStatus.RUNNING,
            Instant.now(),
            null, null
        );

        ExecutionState state = new ExecutionState(
            exec.id(),
            ExecutionStatus.RUNNING,
            new EntityId("n1"),
            Map.of(),
            java.util.Set.of(),
            java.util.Set.of(),
            0
        );

        NodeExecutionContext ctx =
            new NodeExecutionContext(
                exec, state, null, Map.of()
        );

        NodeExecutionResult result =
            executor.execute(node, ctx);

        assertInstanceOf(
            NodeFailed.class, result
        );
        assertEquals(
            "SKILL_TIMEOUT",
            ((NodeFailed) result).code()
        );
    }

    @Test
    void shouldRejectMissingSkillId() {
        SkillInvoker stub = req ->
            CompletableFuture.completedFuture(
                SkillResult.success(
                    SkillReference.of(
                        "s", "1.0.0"
                    ),
                    Map.of()
                )
            );

        SkillNodeExecutor executor =
            new SkillNodeExecutor(stub);

        ExecutionNode node = new ExecutionNode(
            new EntityId("n1"),
            NodeType.SKILL,
            Map.of(), // no skillId
            Map.of()
        );

        Execution exec = new Execution(
            ExecutionId.generate(),
            new EntityId("g1"),
            new Version("1.0.0"),
            "fp",
            ExecutionStatus.RUNNING,
            Instant.now(),
            null, null
        );

        ExecutionState state = new ExecutionState(
            exec.id(),
            ExecutionStatus.RUNNING,
            new EntityId("n1"),
            Map.of(),
            java.util.Set.of(),
            java.util.Set.of(),
            0
        );

        NodeExecutionContext ctx =
            new NodeExecutionContext(
                exec, state, null, Map.of()
        );

        NodeExecutionResult result =
            executor.execute(node, ctx);

        assertInstanceOf(
            NodeFailed.class, result
        );
    }

    @Test
    void shouldRejectNullInvoker() {
        assertThrows(
            IllegalArgumentException.class,
            () -> new SkillNodeExecutor(null)
        );
    }

    @Test
    void shouldUseDefaultVersion()
        throws Exception
    {
        SkillReference[] captured =
            new SkillReference[1];

        SkillInvoker stub = req -> {
            captured[0] = req.skill();
            return CompletableFuture
                .completedFuture(
                    SkillResult.success(
                        req.skill(),
                        Map.of()
                    )
                );
        };

        SkillNodeExecutor executor =
            new SkillNodeExecutor(stub);

        ExecutionNode node = new ExecutionNode(
            new EntityId("n1"),
            NodeType.SKILL,
            Map.of("skillId", "s1"),
            // no skillVersion → default
            // 1.0.0
            Map.of()
        );

        Execution exec = new Execution(
            ExecutionId.generate(),
            new EntityId("g1"),
            new Version("1.0.0"),
            "fp",
            ExecutionStatus.RUNNING,
            Instant.now(),
            null, null
        );

        ExecutionState state = new ExecutionState(
            exec.id(),
            ExecutionStatus.RUNNING,
            new EntityId("n1"),
            Map.of(),
            java.util.Set.of(),
            java.util.Set.of(),
            0
        );

        NodeExecutionContext ctx =
            new NodeExecutionContext(
                exec, state, null, Map.of()
        );

        executor.execute(node, ctx);

        assertNotNull(captured[0]);
        assertEquals("1.0.0",
            captured[0].version().value()
        );
    }
}
