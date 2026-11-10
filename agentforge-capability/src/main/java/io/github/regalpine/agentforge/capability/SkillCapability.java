package io.github.regalpine.agentforge.capability;

import io.github.regalpine.agentforge.kernel.CapabilityRef;
import io.github.regalpine.agentforge.kernel.Skill;
import io.github.regalpine.agentforge.kernel.SkillExecutionStrategy;

import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/**
 * Skill → Capability 调用适配器.
 * AF-SKILL-01 §6: SkillCapability.
 * AF-AD-15 §6.3: DIRECT_CAPABILITY 策略路径.
 *
 * <p>Bridges a Skill invocation into the
 * existing Capability Invocation Pipeline.
 * Used when a Skill's execution strategy
 * is DIRECT_CAPABILITY.
 *
 * <p>This is NOT a new Runtime — it delegates
 * entirely to {@link InvocationPipeline}.
 */
public class SkillCapability {

    private final InvocationPipeline pipeline;

    public SkillCapability(InvocationPipeline pipeline) {
        this.pipeline = Objects.requireNonNull(
            pipeline, "pipeline"
        );
    }

    /**
     * Invoke a Skill through the Capability
     * Invocation Pipeline.
     *
     * <p>Converts the Skill's input arguments
     * into a {@link CapabilityInvocation},
     * executes through the pipeline, and
     * returns the raw result.
     *
     * @param skill     the Skill definition
     * @param arguments input arguments
     * @param context   invocation context
     * @return the pipeline result
     */
    public PipelineResult invoke(
        Skill skill,
        Map<String, Object> arguments,
        InvocationContext context
    ) {
        Objects.requireNonNull(skill, "skill");
        Objects.requireNonNull(context, "context");

        if (skill.executionStrategy()
            != SkillExecutionStrategy.DIRECT_CAPABILITY
        ) {
            throw new IllegalArgumentException(
                "Skill \"" + skill.name() + "\" "
                    + "does not use "
                    + "DIRECT_CAPABILITY strategy"
            );
        }

        CapabilityRef capRef =
            resolveCapabilityRef(skill);

        CapabilityInvocation invocation =
            new CapabilityInvocation(
                buildInvocationId(skill),
                capRef.id(),
                capRef.version(),
                arguments == null
                    ? Map.of()
                    : Map.copyOf(arguments),
                context
            );

        return pipeline.execute(invocation);
    }

    /**
     * Resolve the Capability reference
     * from a Skill's dependencies.
     * Uses the first declared capability
     * dependency.
     */
    private CapabilityRef resolveCapabilityRef(
        Skill skill
    ) {
        var caps =
            skill.dependencies().capabilities();

        if (caps.isEmpty()) {
            throw new IllegalStateException(
                "DIRECT_CAPABILITY Skill "
                    + skill.name()
                    + " declares no capability "
                    + "dependencies"
            );
        }

        return caps.iterator().next();
    }

    private String buildInvocationId(Skill skill) {
        return "skill-"
            + skill.skillId().value()
            + "-"
            + UUID.randomUUID();
    }
}
