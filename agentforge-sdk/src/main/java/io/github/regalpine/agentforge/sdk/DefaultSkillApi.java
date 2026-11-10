package io.github.regalpine.agentforge.sdk;

import io.github.regalpine.agentforge.capability.SkillRegistry;
import io.github.regalpine.agentforge.kernel.SkillInvocationRequest;
import io.github.regalpine.agentforge.skill.governance.SkillQuery;
import io.github.regalpine.agentforge.kernel.SkillReference;
import io.github.regalpine.agentforge.skill.governance.SkillResult;
import io.github.regalpine.agentforge.runtime.SkillInvoker;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletionStage;

/**
 * Embedded SkillApi implementation.
 * AF-SKILL-04 §22: Embedded Mode.
 *
 * <p>Directly composes SkillInvoker +
 * SkillRegistry. Does not go through
 * any network layer.
 *
 * <p>This is a Facade — it does not expose
 * Worker / Lease / Queue / Scheduler.
 */
final class DefaultSkillApi implements SkillApi {

    private final SkillRegistry registry;
    private final SkillInvoker invoker;

    DefaultSkillApi(
        SkillRegistry registry,
        SkillInvoker invoker
    ) {
        this.registry = Objects.requireNonNull(
            registry, "registry"
        );
        this.invoker = Objects.requireNonNull(
            invoker, "invoker"
        );
    }

    @Override
    public SkillDescriptor resolve(
        String skillId,
        String version
    ) {
        Objects.requireNonNull(skillId, "skillId");
        Objects.requireNonNull(version, "version");

        SkillReference ref = SkillReference.of(
            skillId, version
        );

        io.github.regalpine.agentforge.kernel.SkillDescriptor kernel =
            registry.resolve(ref).orElseThrow(
                () -> new ResolutionException(
                    "Skill", skillId
                )
            );

        return io.github.regalpine.agentforge.sdk.SkillDescriptor.from(kernel);
    }

    @Override
    public List<SkillDescriptor> discover(
        SkillDiscoveryQuery query
    ) {
        SkillQuery kernelQuery = query == null
            ? SkillQuery.all()
            : query.toKernelQuery();

        return registry.discover(kernelQuery)
            .stream()
            .map(io.github.regalpine.agentforge.sdk.SkillDescriptor::from)
            .toList();
    }

    @Override
    public SkillExecutionResult invoke(
        String skillId,
        String version,
        Map<String, Object> input
    ) {
        SkillReference ref = SkillReference.of(
            skillId, version
        );

        SkillInvocationRequest request =
            new SkillInvocationRequest(
                ref,
                input != null ? input : Map.of()
            );

        CompletionStage<SkillResult> stage =
            invoker.invoke(request);

        SkillResult result = stage
            .toCompletableFuture()
            .join();

        return SkillExecutionResult.from(result);
    }

    @Override
    public CompletionStage<SkillExecutionResult> invokeAsync(
        String skillId,
        String version,
        Map<String, Object> input
    ) {
        SkillReference ref = SkillReference.of(
            skillId, version
        );

        SkillInvocationRequest request =
            new SkillInvocationRequest(
                ref,
                input != null ? input : Map.of()
            );

        return invoker.invoke(request)
            .thenApply(SkillExecutionResult::from);
    }

    @Override
    public List<String> listSkillIds() {
        return registry.list()
            .stream()
            .map(d -> d.skillId().value())
            .distinct()
            .toList();
    }
}
