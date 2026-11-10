package io.github.regalpine.agentforge.capability;

import io.github.regalpine.agentforge.kernel.SkillDescriptor;
import io.github.regalpine.agentforge.skill.governance.SkillDigest;
import io.github.regalpine.agentforge.skill.governance.SkillLifecycleState;
import io.github.regalpine.agentforge.skill.governance.SkillPackageMetadata;
import io.github.regalpine.agentforge.skill.governance.SkillQuery;
import io.github.regalpine.agentforge.kernel.SkillReference;
import io.github.regalpine.agentforge.skill.governance.SkillRegistration;

import java.util.List;
import java.util.Optional;

/**
 * Skill Registry — 资产目录服务.
 * AF-SKILL-02 §6: SkillRegistry.
 * AF-AD-15 §5.1: Registry 核心接口.
 *
 * <p>Registry 是资产目录与解析服务，
 * 永远不是执行引擎。禁止
 * {@code Registry → execute()}。
 *
 * <p>D：Registry 接口不绑定存储实现。
 * 可适配 InMemory（Local）、PostgreSQL
 * （Application）或企业级 Registry。
 */
public interface SkillRegistry {

    /**
     * Register a Skill package.
     *
     * @param metadata Skill package metadata
     * @param digest   artifact digest
     * @return registration result
     * @throws IllegalArgumentException
     *   if id+version already registered
     */
    SkillRegistration register(
        SkillPackageMetadata metadata,
        SkillDigest digest
    );

    /**
     * Resolve a Skill by reference.
     *
     * @return descriptor if found
     */
    Optional<SkillDescriptor> resolve(
        SkillReference reference
    );

    /**
     * Discover Skills matching query.
     */
    List<SkillDescriptor> discover(
        SkillQuery query
    );

    /**
     * Get lifecycle state of a Skill.
     */
    SkillLifecycleState state(
        SkillReference reference
    );

    /**
     * Transition lifecycle state.
     *
     * @throws IllegalStateException
     *   if transition not allowed
     */
    void transition(
        SkillReference reference,
        SkillLifecycleState target
    );

    /**
     * Deprecate a Skill version.
     */
    default void deprecate(
        SkillReference reference
    ) {
        transition(
            reference,
            SkillLifecycleState.DEPRECATED
        );
    }

    /**
     * Retire a Skill version.
     */
    default void retire(
        SkillReference reference
    ) {
        transition(
            reference,
            SkillLifecycleState.RETIRED
        );
    }

    /**
     * Revoke a Skill (emergency).
     */
    default void revoke(
        SkillReference reference
    ) {
        transition(
            reference,
            SkillLifecycleState.REVOKED
        );
    }

    /**
     * Get package metadata.
     */
    Optional<SkillPackageMetadata> metadata(
        SkillReference reference
    );

    /**
     * Get artifact digest.
     */
    Optional<SkillDigest> digest(
        SkillReference reference
    );

    /**
     * List all registered Skills.
     */
    List<SkillDescriptor> list();

    /**
     * Unregister a Skill (admin only).
     */
    void unregister(SkillReference reference);
}
