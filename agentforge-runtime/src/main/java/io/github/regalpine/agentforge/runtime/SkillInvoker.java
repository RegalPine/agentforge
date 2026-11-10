package io.github.regalpine.agentforge.runtime;

import io.github.regalpine.agentforge.event.Execution;
import io.github.regalpine.agentforge.kernel.SkillInvocationRequest;
import io.github.regalpine.agentforge.skill.governance.SkillResult;

import java.util.concurrent.CompletionStage;

/**
 * Skill 调用 Facade.
 * AF-SKILL-01 §6: SkillInvoker.
 * AF-AD-15 §6.2: Facade 不是新 Runtime.
 *
 * <p>Unified entry point for all Skill
 * invocations. All four sources (Agent Loop
 * / Graph Node / Application SDK / Parent
 * Skill) use this interface.
 *
 * <p>SkillInvoker is a Facade, NOT a new
 * Runtime. It delegates to existing
 * Execution infrastructure based on the
 * Skill's execution strategy.
 */
public interface SkillInvoker {

    /**
     * Invoke a Skill asynchronously.
     *
     * @param request the invocation request
     * @return a CompletionStage that will
     *         produce the SkillResult
     */
    CompletionStage<SkillResult> invoke(
        SkillInvocationRequest request
    );
}
