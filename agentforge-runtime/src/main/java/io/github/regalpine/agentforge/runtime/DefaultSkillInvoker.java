package io.github.regalpine.agentforge.runtime;

import io.github.regalpine.agentforge.capability.SkillCapability;
import io.github.regalpine.agentforge.kernel.ResolvedSkill;
import io.github.regalpine.agentforge.skill.governance.SkillErrorCode;
import io.github.regalpine.agentforge.kernel.SkillExecutionStrategy;
import io.github.regalpine.agentforge.kernel.SkillInvocationRequest;
import io.github.regalpine.agentforge.skill.governance.SkillResult;
import io.github.regalpine.agentforge.skill.governance.SkillResultStatus;

import java.time.Instant;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

/**
 * Default SkillInvoker implementation.
 * AF-SKILL-01 §6: SkillInvoker Facade.
 * AF-AD-15 §6.1-§6.3: 调用管线 + 策略路由.
 *
 * <p>This is a Facade, NOT a new Runtime.
 * It resolves the Skill, selects the
 * execution strategy, and delegates to
 * existing infrastructure.
 *
 * <p>Pipeline:
 * <pre>
 * Receive → Resolve → Version Resolve
 * → Input Validate → Strategy Select
 * → Execute → Output Validate → Return
 * </pre>
 */
public final class DefaultSkillInvoker
    implements SkillInvoker {

    private final SkillResolver resolver;
    private final SkillCapability skillCapability;

    public DefaultSkillInvoker(
        SkillResolver resolver,
        SkillCapability skillCapability
    ) {
        this.resolver =
            Objects.requireNonNull(
                resolver, "resolver"
            );
        this.skillCapability =
            Objects.requireNonNull(
                skillCapability, "skillCapability"
            );
    }

    @Override
    public CompletionStage<SkillResult> invoke(
        SkillInvocationRequest request
    ) {
        Objects.requireNonNull(
            request, "request"
        );

        return CompletableFuture.supplyAsync(
            () -> executeSync(request)
        );
    }

    /**
     * Synchronous execution for internal use.
     * Follows the full pipeline.
     */
    SkillResult executeSync(
        SkillInvocationRequest request
    ) {
        // 1. RESOLVE
        ResolvedSkill resolved;
        try {
            resolved = resolver.resolve(
                request.skill()
            );
        } catch (SkillResolutionException e) {
            return SkillResult.failed(
                request.skill(),
                SkillErrorCode.SKILL_NOT_FOUND,
                e.getMessage()
            );
        }

        // 2. INPUT VALIDATE
        if (!validateInput(
            resolved, request.input()
        )) {
            return SkillResult.failed(
                request.skill(),
                SkillErrorCode.SKILL_INPUT_INVALID,
                "Input validation failed for Skill: "
                    + resolved.skill().name()
            );
        }

        // 3. STRATEGY SELECT + EXECUTE
        return switch (
            resolved.strategy()
        ) {
            case DIRECT_CAPABILITY ->
                executeDirectCapability(
                    resolved, request
                );
            case GRAPH ->
                executeGraph(resolved, request);
            case AGENT ->
                executeAgent(resolved, request);
            case COMPOSITE_SKILL ->
                executeComposite(
                    resolved, request
                );
        };
    }

    /**
     * DIRECT_CAPABILITY strategy:
     * Skill → SkillCapability → Pipeline.
     */
    private SkillResult executeDirectCapability(
        ResolvedSkill resolved,
        SkillInvocationRequest request
    ) {
        try {
            io.github.regalpine.agentforge.capability
                .InvocationContext ctx =
                    new io.github.regalpine.agentforge
                        .capability.InvocationContext(
                            buildInvocationId(request),
                            request.tenantId() != null
                                ? request.tenantId()
                                : "default",
                            Map.of()
                        );

            var result = skillCapability.invoke(
                resolved.skill(),
                request.input(),
                ctx
            );

            if (result.isSuccess()) {
                var success =
                    (io.github.regalpine.agentforge
                        .capability.CapabilitySuccess)
                        result.result();

                return SkillResult.success(
                    request.skill(),
                    success.output()
                );
            } else {
                var failure =
                    (io.github.regalpine.agentforge
                        .capability.CapabilityFailure)
                        result.result();

                return SkillResult.failed(
                    request.skill(),
                    SkillErrorCode
                        .SKILL_EXECUTION_FAILED,
                    failure.message()
                );
            }
        } catch (Exception e) {
            return SkillResult.failed(
                request.skill(),
                SkillErrorCode
                    .SKILL_EXECUTION_FAILED,
                e.getMessage()
            );
        }
    }

    /**
     * GRAPH strategy: stub.
     * T-S2.8: Full implementation delegates
     * to GraphRuntime.
     */
    private SkillResult executeGraph(
        ResolvedSkill resolved,
        SkillInvocationRequest request
    ) {
        // D: GRAPH strategy delegates to
        // existing ExecutionEngine via
        // GraphReference. Full implementation
        // in S2 integration phase.
        return SkillResult.failed(
            request.skill(),
            SkillErrorCode
                .SKILL_EXECUTION_FAILED,
            "GRAPH strategy not yet "
                + "implemented for Skill: "
                + resolved.skill().name()
        );
    }

    /**
     * AGENT strategy: stub.
     * T-S2.9: Full implementation delegates
     * to AgentRuntime.
     */
    private SkillResult executeAgent(
        ResolvedSkill resolved,
        SkillInvocationRequest request
    ) {
        // D: AGENT strategy delegates to
        // existing AgentLoop via
        // AgentReference. Full implementation
        // in S2 integration phase.
        return SkillResult.failed(
            request.skill(),
            SkillErrorCode
                .SKILL_EXECUTION_FAILED,
            "AGENT strategy not yet "
                + "implemented for Skill: "
                + resolved.skill().name()
        );
    }

    /**
     * COMPOSITE_SKILL strategy: stub.
     * T-S2.10: Full implementation performs
     * sequential/combined Skill invocation.
     */
    private SkillResult executeComposite(
        ResolvedSkill resolved,
        SkillInvocationRequest request
    ) {
        // D: COMPOSITE_SKILL strategy
        // invokes child Skills sequentially.
        // Full implementation in S2.
        return SkillResult.failed(
            request.skill(),
            SkillErrorCode
                .SKILL_EXECUTION_FAILED,
            "COMPOSITE_SKILL strategy "
                + "not yet implemented "
                + "for Skill: "
                + resolved.skill().name()
        );
    }

    private boolean validateInput(
        ResolvedSkill resolved,
        Map<String, Object> input
    ) {
        // D: Basic validation — non-null
        // input. Full JSON Schema validation
        // deferred to S3 security phase.
        return input != null;
    }

    private String buildInvocationId(
        SkillInvocationRequest request
    ) {
        return "skill-inv-"
            + request.skill().skillId().value()
            + "-"
            + java.util.UUID.randomUUID();
    }
}
