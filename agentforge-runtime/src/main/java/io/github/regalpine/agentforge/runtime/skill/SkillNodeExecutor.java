package io.github.regalpine.agentforge.runtime.skill;

import io.github.regalpine.agentforge.graph.NodeType;
import io.github.regalpine.agentforge.graph.ir.ExecutionNode;
import io.github.regalpine.agentforge.skill.governance.SkillErrorCode;
import io.github.regalpine.agentforge.kernel.SkillInvocationRequest;
import io.github.regalpine.agentforge.kernel.SkillReference;
import io.github.regalpine.agentforge.skill.governance.SkillResult;
import io.github.regalpine.agentforge.kernel.Version;
import io.github.regalpine.agentforge.runtime.NodeCompleted;
import io.github.regalpine.agentforge.runtime.NodeExecutionResult;
import io.github.regalpine.agentforge.runtime.NodeExecutor;
import io.github.regalpine.agentforge.runtime.NodeExecutionContext;
import io.github.regalpine.agentforge.runtime.NodeFailed;
import io.github.regalpine.agentforge.runtime.SkillInvoker;

import java.util.Map;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * NodeExecutor for SKILL NodeType.
 * AF-SKILL-01 §27: SkillNode 执行.
 * AF-AD-15 §7.2: Graph Skill Node 执行器.
 *
 * <p>Bridges Graph Runtime with SkillInvoker.
 * When a SKILL node is encountered during
 * graph execution, this executor delegates
 * to the {@link SkillInvoker} for the Skill's
 * execution.
 *
 * <p>Graph Runtime only sees the final
 * {@link SkillResult} — it does not
 * understand the internal Skill execution
 * strategy.
 *
 * <p>Skill Node 执行结果通过 StateDelta 返回，
 * 不直接修改 Graph 全局 State。
 * 不创建 Skill Graph Engine.
 */
public final class SkillNodeExecutor
    implements NodeExecutor {

    private final SkillInvoker invoker;
    private final long defaultTimeoutMs;

    public SkillNodeExecutor(
        SkillInvoker invoker
    ) {
        this(invoker, 30_000L);
    }

    public SkillNodeExecutor(
        SkillInvoker invoker,
        long defaultTimeoutMs
    ) {
        if (invoker == null) {
            throw new IllegalArgumentException(
                "invoker cannot be null"
            );
        }
        if (defaultTimeoutMs <= 0) {
            throw new IllegalArgumentException(
                "defaultTimeoutMs must be positive"
            );
        }
        this.invoker = invoker;
        this.defaultTimeoutMs = defaultTimeoutMs;
    }

    @Override
    public NodeExecutionResult execute(
        ExecutionNode node,
        NodeExecutionContext context
    ) {
        if (node.type() != NodeType.SKILL) {
            return new NodeFailed(
                "SKILL_TYPE_MISMATCH",
                "SkillNodeExecutor can only "
                    + "handle SKILL nodes, got: "
                    + node.type(),
                false
            );
        }

        try {
            // Extract Skill reference
            // from config
            SkillReference skillRef =
                extractSkillRef(node);

            // Build invocation request
            SkillInvocationRequest request =
                new SkillInvocationRequest(
                    skillRef,
                    node.config(),
                    context.execution().id()
                        .value(),
                    null,
                    defaultTimeoutMs
                );

            // Invoke through SkillInvoker
            CompletionStage<SkillResult> stage =
                invoker.invoke(request);

            SkillResult result = stage
                .toCompletableFuture()
                .get(
                    defaultTimeoutMs,
                    TimeUnit.MILLISECONDS
                );

            return toNodeResult(result);
        } catch (TimeoutException e) {
            return new NodeFailed(
                SkillErrorCode.SKILL_TIMEOUT
                    .name(),
                "Skill execution timed out",
                true
            );
        } catch (ExecutionException e) {
            return new NodeFailed(
                SkillErrorCode
                    .SKILL_EXECUTION_FAILED
                    .name(),
                e.getCause() != null
                    ? e.getCause().getMessage()
                    : e.getMessage(),
                true
            );
        } catch (Exception e) {
            return new NodeFailed(
                SkillErrorCode
                    .SKILL_EXECUTION_FAILED
                    .name(),
                e.getMessage(),
                true
            );
        }
    }

    /**
     * Extract SkillReference from
     * ExecutionNode config.
     * Config must contain "skillId" and
     * optionally "skillVersion".
     */
    private SkillReference extractSkillRef(
        ExecutionNode node
    ) {
        Map<String, Object> config =
            node.config();

        String skillId =
            (String) config.get("skillId");

        if (skillId == null
            || skillId.isBlank()
        ) {
            throw new IllegalArgumentException(
                "SKILL node config must "
                    + "contain 'skillId'"
            );
        }

        String versionStr =
            (String) config.getOrDefault(
                "skillVersion",
                "1.0.0"
            );

        return SkillReference.of(
            skillId, versionStr
        );
    }

    private NodeExecutionResult toNodeResult(
        SkillResult result
    ) {
        if (result.isSuccess()) {
            return new NodeCompleted(
                result.output()
            );
        }

        String code =
            result.errorCode() != null
                ? result.errorCode().name()
                : "SKILL_FAILED";

        String msg =
            result.errorMessage() != null
                ? result.errorMessage()
                : "Skill execution failed";

        return new NodeFailed(code, msg, false);
    }
}
