package io.github.regalpine.agentforge.server.service;

import io.github.regalpine.agentforge.capability.SkillRegistry;
import io.github.regalpine.agentforge.kernel.SkillDescriptor;
import io.github.regalpine.agentforge.skill.governance.SkillDigest;
import io.github.regalpine.agentforge.kernel.SkillInvocationRequest;
import io.github.regalpine.agentforge.skill.governance.SkillLifecycleState;
import io.github.regalpine.agentforge.skill.governance.SkillPackageMetadata;
import io.github.regalpine.agentforge.skill.governance.SkillQuery;
import io.github.regalpine.agentforge.kernel.SkillReference;
import io.github.regalpine.agentforge.skill.governance.SkillRegistration;
import io.github.regalpine.agentforge.skill.governance.SkillResult;
import io.github.regalpine.agentforge.runtime.SkillInvoker;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletionStage;

/**
 * Skill REST API 服务层.
 * AF-SKILL-04 §44-§49: Skill REST API.
 * AF-AD-15 §11.2: Server Skill API.
 *
 * <p>Provides CRUD, lifecycle, and
 * invocation operations for the REST
 * layer. Delegates to SkillRegistry
 * and SkillInvoker.
 *
 * <p>N: RBAC and Tenant Isolation are
 * enforced at the HTTP handler layer,
 * not here.
 */
public final class SkillService {

    private final SkillRegistry registry;
    private final SkillInvoker invoker;

    public SkillService(
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

    // ── Query ─────────────────

    /**
     * List all Skills.
     * GET /api/v1/skills
     */
    public List<SkillDescriptor> listSkills() {
        return registry.list();
    }

    /**
     * Get a specific Skill.
     * GET /api/v1/skills/{id}
     */
    public Optional<SkillDescriptor> getSkill(
        String skillId
    ) {
        Objects.requireNonNull(skillId);

        return registry.list().stream()
            .filter(d ->
                d.skillId().value().equals(skillId)
            )
            .findFirst();
    }

    /**
     * Get Skill versions.
     * GET /api/v1/skills/{id}/versions
     */
    public List<SkillDescriptor> getSkillVersions(
        String skillId
    ) {
        Objects.requireNonNull(skillId);

        return registry.list().stream()
            .filter(d ->
                d.skillId().value().equals(skillId)
            )
            .toList();
    }

    /**
     * Discover Skills by query.
     * GET /api/v1/skills?q=...
     */
    public List<SkillDescriptor> discoverSkills(
        SkillQuery query
    ) {
        return registry.discover(
            query != null ? query : SkillQuery.all()
        );
    }

    // ── Lifecycle ─────────────

    /**
     * Get lifecycle state.
     */
    public SkillLifecycleState getState(
        String skillId,
        String version
    ) {
        SkillReference ref = SkillReference.of(
            skillId, version
        );
        return registry.state(ref);
    }

    /**
     * Transition lifecycle.
     * PUT /api/v1/skills/{id}/versions/{v}/lifecycle
     */
    public void transition(
        String skillId,
        String version,
        SkillLifecycleState target
    ) {
        SkillReference ref = SkillReference.of(
            skillId, version
        );
        registry.transition(ref, target);
    }

    // ── Invocation ────────────

    /**
     * Invoke a Skill.
     * POST /api/v1/skills/{id}/invoke
     */
    public CompletionStage<SkillResult> invoke(
        String skillId,
        String version,
        Map<String, Object> input,
        String tenantId
    ) {
        SkillReference ref = SkillReference.of(
            skillId, version
        );

        SkillInvocationRequest request =
            new SkillInvocationRequest(
                ref,
                input != null ? input : Map.of(),
                "rest-api",
                tenantId,
                30_000L
            );

        return invoker.invoke(request);
    }
}
