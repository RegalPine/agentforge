package io.github.regalpine.agentforge.sdk;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

/**
 * No-op SkillApi placeholder.
 * Used when no SkillRegistry/Invoker
 * is configured.
 */
final class NoOpSkillApi implements SkillApi {

    @Override
    public SkillDescriptor resolve(
        String skillId, String version
    ) {
        throw new ResolutionException(
            "Skill", skillId
        );
    }

    @Override
    public List<SkillDescriptor> discover(
        SkillDiscoveryQuery query
    ) {
        return List.of();
    }

    @Override
    public SkillExecutionResult invoke(
        String skillId, String version,
        Map<String, Object> input
    ) {
        return SkillExecutionResult.failure(
            skillId, version,
            "SKILL_NOT_CONFIGURED",
            "Skill API not configured"
        );
    }

    @Override
    public CompletionStage<SkillExecutionResult> invokeAsync(
        String skillId, String version,
        Map<String, Object> input
    ) {
        return CompletableFuture.completedFuture(
            invoke(skillId, version, input)
        );
    }

    @Override
    public List<String> listSkillIds() {
        return List.of();
    }
}
