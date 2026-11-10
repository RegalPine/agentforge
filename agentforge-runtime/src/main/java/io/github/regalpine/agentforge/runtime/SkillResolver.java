package io.github.regalpine.agentforge.runtime;

import io.github.regalpine.agentforge.kernel.ResolvedSkill;
import io.github.regalpine.agentforge.kernel.SkillReference;

/**
 * Skill 解析器.
 * AF-SKILL-01 §6: SkillResolver.
 * AF-AD-15 §6.2: Registry 解析并绑定版本.
 *
 * <p>Resolves a SkillReference against
 * the SkillRegistry, binding the version
 * and returning the full Skill definition.
 */
public interface SkillResolver {

    /**
     * Resolve a Skill by reference.
     *
     * @param reference the Skill reference
     * @return the resolved Skill with bound
     *         version and strategy
     * @throws SkillResolutionException
     *   if the Skill cannot be resolved
     */
    ResolvedSkill resolve(SkillReference reference);
}
