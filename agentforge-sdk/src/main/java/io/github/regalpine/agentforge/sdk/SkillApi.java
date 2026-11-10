package io.github.regalpine.agentforge.sdk;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletionStage;

/**
 * Skill API Facade.
 * AF-SKILL-04 §21-§25: SkillClient.
 * AF-AD-15 §11.1: SDK SkillClient.
 *
 * <p>Provides developer-facing access to
 * Skill operations: resolve, discover,
 * invoke. This is a Facade — it does not
 * expose Worker / Lease / Queue / Scheduler
 * internals.
 *
 * <p>Two modes:
 * <ul>
 *   <li>Embedded: directly composes
 *       SkillInvoker + SkillRegistry</li>
 *   <li>Remote: delegates to Server REST
 *       API (not implemented in this class)</li>
 * </ul>
 */
public interface SkillApi {

    /**
     * Resolve a Skill by ID and version.
     *
     * @param skillId the skill identifier
     * @param version the version string
     * @return the skill descriptor
     * @throws ResolutionException if not found
     */
    SkillDescriptor resolve(
        String skillId,
        String version
    );

    /**
     * Discover Skills matching criteria.
     *
     * @param query optional filter (null = all)
     * @return list of matching descriptors
     */
    List<SkillDescriptor> discover(
        SkillDiscoveryQuery query
    );

    /**
     * Invoke a Skill synchronously.
     *
     * @param skillId   the skill identifier
     * @param version   the version string
     * @param input     the input parameters
     * @return the execution result
     */
    SkillExecutionResult invoke(
        String skillId,
        String version,
        Map<String, Object> input
    );

    /**
     * Invoke a Skill asynchronously.
     *
     * @param skillId   the skill identifier
     * @param version   the version string
     * @param input     the input parameters
     * @return a CompletionStage producing
     *         the execution result
     */
    CompletionStage<SkillExecutionResult> invokeAsync(
        String skillId,
        String version,
        Map<String, Object> input
    );

    /**
     * List all registered Skill IDs.
     */
    List<String> listSkillIds();
}
