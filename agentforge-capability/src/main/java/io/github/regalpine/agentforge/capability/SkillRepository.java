package io.github.regalpine.agentforge.capability;

import io.github.regalpine.agentforge.skill.governance.SkillPackageMetadata;
import io.github.regalpine.agentforge.skill.governance.SkillQuery;
import io.github.regalpine.agentforge.kernel.SkillReference;

import java.util.List;
import java.util.Optional;

/**
 * Skill Repository SPI 端口.
 * AF-SKILL-02 §7: SkillRepository.
 * AF-AD-15 §5.1: Repository 不绑定存储.
 *
 * <p>Storage-agnostic SPI for Skill
 * metadata persistence. Implementations:
 * <ul>
 *   <li>InMemory — unit tests, CLI, Demo</li>
 *   <li>Filesystem — local Package loading</li>
 *   <li>PostgreSQL — Application Registry</li>
 * </ul>
 */
public interface SkillRepository {

    /**
     * Save Skill metadata.
     *
     * @throws IllegalArgumentException
     *   if id+version already exists
     */
    void save(SkillPackageMetadata metadata);

    /**
     * Find Skill metadata by reference.
     */
    Optional<SkillPackageMetadata> find(
        SkillReference reference
    );

    /**
     * Query Skill metadata.
     */
    List<SkillPackageMetadata> query(
        SkillQuery query
    );

    /**
     * Delete Skill metadata.
     */
    void delete(SkillReference reference);

    /**
     * Check if a Skill exists.
     */
    boolean exists(SkillReference reference);

    /**
     * Count all registered Skills.
     */
    int count();
}
