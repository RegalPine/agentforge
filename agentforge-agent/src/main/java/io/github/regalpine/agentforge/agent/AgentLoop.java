package io.github.regalpine.agentforge.agent;

import io.github.regalpine.agentforge.capability.*;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Enhanced Agent Loop with budget tracking, stagnation detection, and policy gating.
 * <p>
 * The Agent Loop implements the core Observe → Decision → Action → Observation cycle
 * with enterprise-grade controls:
 * <ul>
 *   <li>Budget tracking (iterations, duration, tokens, tool calls, cost)</li>
 *   <li>Stagnation detection (repeated identical decisions)</li>
 *   <li>Policy gate (pre-action validation)</li>
 *   <li>Step recording for observability</li>
 * </ul>
 */
public final class AgentLoop {

    private final AgentDecisionEngine decisionEngine;
    private final BudgetTracker budgetTracker;
    private final StagnationDetector stagnationDetector;
    private final PolicyGate policyGate;
    // G3-3: action execution port for real capability invocation
    private final ActionExecutionPort actionPort;
    private final List<LoopStep> steps;

    private AgentContext context;
    private StopCondition stopCondition;

    /**
     * Create an AgentLoop with all components.
     *
     * @param decisionEngine      the decision engine
     * @param budget              the loop budget
     * @param stagnationDetector  the stagnation detector
     * @param policyGate          the policy gate
     * @param initialContext      the initial agent context
     */
    public AgentLoop(
        AgentDecisionEngine decisionEngine,
        LoopBudget budget,
        StagnationDetector stagnationDetector,
        PolicyGate policyGate,
        AgentContext initialContext
    ) {
        this(decisionEngine, budget, stagnationDetector, policyGate, initialContext, ActionExecutionPort.noOp());
    }

    /**
     * G3-3: create an AgentLoop with action port.
     *
     * @param decisionEngine      the decision engine
     * @param budget              the loop budget
     * @param stagnationDetector  the stagnation detector
     * @param policyGate          the policy gate
     * @param initialContext      the initial agent context
     * @param actionPort          the action execution port
     */
    public AgentLoop(
        AgentDecisionEngine decisionEngine,
        LoopBudget budget,
        StagnationDetector stagnationDetector,
        PolicyGate policyGate,
        AgentContext initialContext,
        ActionExecutionPort actionPort
    ) {
        this.decisionEngine = Objects.requireNonNull(decisionEngine, "decisionEngine");
        this.budgetTracker = new BudgetTracker(budget);
        this.stagnationDetector = stagnationDetector != null ? stagnationDetector : new StagnationDetector();
        this.policyGate = policyGate != null ? policyGate : PolicyGate.permissive();
        this.context = Objects.requireNonNull(initialContext, "initialContext");
        this.actionPort = Objects.requireNonNull(actionPort, "actionPort");
        this.steps = new ArrayList<>();
    }

    /**
     * Create an AgentLoop with default settings.
     *
     * @param decisionEngine the decision engine
     * @param initialContext the initial agent context
     * @return a new AgentLoop
     */
    public static AgentLoop withDefaults(AgentDecisionEngine decisionEngine, AgentContext initialContext) {
        return new AgentLoop(decisionEngine, LoopBudget.defaults(), new StagnationDetector(), PolicyGate.permissive(), initialContext, ActionExecutionPort.noOp());
    }

    /**
     * G3-3: create with defaults and action port.
     */
    public static AgentLoop withDefaults(
        AgentDecisionEngine decisionEngine,
        AgentContext initialContext,
        ActionExecutionPort actionPort
    ) {
        return new AgentLoop(decisionEngine, LoopBudget.defaults(), new StagnationDetector(), PolicyGate.permissive(), initialContext, actionPort);
    }

    /**
     * Run the Agent Loop until completion or budget exhaustion.
     *
     * @return the final AgentResult
     */
    public AgentResult run() {
        while (true) {
            // 1. Budget check
            Optional<StopCondition> budgetExhausted = budgetTracker.checkBudget();
            if (budgetExhausted.isPresent()) {
                stopCondition = budgetExhausted.get();
                return buildResult(AgentResultStatus.FAILED, Map.of(), Map.of(
                    "errorCode", stopCondition.reason().name(),
                    "errorMessage", stopCondition.detail() != null ? stopCondition.detail() : "Budget exhausted"
                ));
            }

            // 2. Record iteration
            long iteration = budgetTracker.recordIteration();

            // 3. Observe phase — build decision context
            AgentDecisionContext decisionContext = new AgentDecisionContext(
                null, // agent reference
                context,
                List.of(), // capabilities
                AgentRuntimePolicy.defaults()
            );

            // 4. Decision phase
            AgentDecision decision;
            try {
                decision = decisionEngine.decide(decisionContext);
            } catch (Exception e) {
                stopCondition = StopCondition.of(StopCondition.StopReason.FAILED, "Decision engine error: " + e.getMessage());
                return buildResult(AgentResultStatus.FAILED, Map.of(), Map.of(
                    "errorCode", "DECISION_ENGINE_ERROR",
                    "errorMessage", e.getMessage() != null ? e.getMessage() : "Unknown error"
                ));
            }

            // 5. Policy gate check
            Optional<StopCondition> policyBlocked = policyGate.check(decision, decisionContext);
            if (policyBlocked.isPresent()) {
                stopCondition = policyBlocked.get();
                return buildResult(AgentResultStatus.FAILED, Map.of(), Map.of(
                    "errorCode", "POLICY_BLOCKED",
                    "errorMessage", stopCondition.detail() != null ? stopCondition.detail() : "Policy blocked"
                ));
            }

            // 6. Handle decision
            AgentResult result = handleDecision(decision, iteration);
            if (result != null) {
                return result;
            }
        }
    }

    private AgentResult handleDecision(AgentDecision decision, long iteration) {
        Instant stepStart = Instant.now();

        if (decision instanceof FinishDecision fd) {
            stopCondition = StopCondition.of(StopCondition.StopReason.FINISHED);
            recordStep(iteration, decision, "FINISH", stepStart);
            return buildResult(AgentResultStatus.COMPLETED, fd.outputs());
        }

        if (decision instanceof WaitDecision wd) {
            stopCondition = StopCondition.of(StopCondition.StopReason.WAITING, "Wait key: " + wd.waitKey());
            recordStep(iteration, decision, "WAIT", stepStart);
            return buildResult(AgentResultStatus.WAITING, Map.of("waitKey", wd.waitKey()));
        }

        if (decision instanceof FailDecision fd) {
            stopCondition = StopCondition.of(StopCondition.StopReason.FAILED, fd.code() + ": " + fd.message());
            recordStep(iteration, decision, "FAIL", stepStart);
            return buildResult(AgentResultStatus.FAILED, Map.of(), Map.of(
                "errorCode", fd.code(),
                "errorMessage", fd.message()
            ));
        }

        if (decision instanceof CapabilityDecision cd) {
            budgetTracker.recordToolCall();
            budgetTracker.recordModelCall();

            String signature = cd.capabilityId();
            Optional<StopCondition> stagnation = stagnationDetector.record(signature);
            if (stagnation.isPresent()) {
                stopCondition = stagnation.get();
                recordStep(iteration, decision, "STAGNATION", stepStart);
                return buildResult(AgentResultStatus.FAILED, Map.of(), Map.of(
                    "errorCode", "STAGNATION",
                    "errorMessage", stopCondition.detail() != null ? stopCondition.detail() : "Stagnation detected"
                ));
            }

            // G3-3: actually invoke the capability
            CapabilityResult capResult = actionPort.execute(cd);

            // Record observation with real result
            AgentObservation observation = new AgentObservation(
                cd.capabilityId() + "-" + iteration,
                capResult,
                Instant.now()
            );
            List<AgentObservation> updatedObs = new ArrayList<>(context.observations());
            updatedObs.add(observation);

            // G3-3: store result in capability results
            Map<String, Object> updatedResults = new HashMap<>(context.capabilityResults());
            updatedResults.put(cd.capabilityId(), resultToMap(capResult));

            context = new AgentContext(
                context.input(),
                context.goal(),
                context.variables(),
                updatedObs,
                Map.copyOf(updatedResults),
                context.metadata()
            );

            // G3-3: handle waiting/cancelled results
            if (capResult instanceof CapabilityWaiting w) {
                stopCondition = StopCondition.of(StopCondition.StopReason.WAITING, "Capability waiting: " + w.reason());
                recordStep(iteration, decision, "CAPABILITY_WAIT", stepStart);
                return buildResult(AgentResultStatus.WAITING, Map.of("waitKey", w.reason()));
            }
            if (capResult instanceof CapabilityCancelled c) {
                stopCondition = StopCondition.of(StopCondition.StopReason.FAILED, "Capability cancelled: " + c.reason());
                recordStep(iteration, decision, "CAPABILITY_CANCEL", stepStart);
                return buildResult(AgentResultStatus.CANCELLED, Map.of(), Map.of("reason", c.reason()));
            }
            if (capResult instanceof CapabilityFailure f) {
                // Record failure but continue loop (agent may recover)
                recordStep(iteration, decision, "CAPABILITY_FAIL", stepStart);
                return null; // continue loop
            }

            recordStep(iteration, decision, "CAPABILITY", stepStart);
            return null; // continue loop
        }

        if (decision instanceof SkillDecision sd) {
            budgetTracker.recordToolCall();
            budgetTracker.recordModelCall();

            String signature = "skill:" + sd.skillId();
            Optional<StopCondition> stagnation = stagnationDetector.record(signature);
            if (stagnation.isPresent()) {
                stopCondition = stagnation.get();
                recordStep(iteration, decision, "STAGNATION", stepStart);
                return buildResult(AgentResultStatus.FAILED, Map.of(), Map.of(
                    "errorCode", "STAGNATION",
                    "errorMessage", stopCondition.detail() != null ? stopCondition.detail() : "Stagnation detected"
                ));
            }

            // Record Skill observation with
            // trustLevel = DERIVED
            AgentObservation observation = new AgentObservation(
                "skill-" + sd.skillId() + "-" + iteration,
                null,
                Instant.now(),
                AgentObservation.SourceType.SKILL,
                AgentObservation.TrustLevel.DERIVED
            );
            List<AgentObservation> updatedObs = new ArrayList<>(context.observations());
            updatedObs.add(observation);

            Map<String, Object> skillResults = new java.util.HashMap<>(context.capabilityResults());
            skillResults.put(
                "skill:" + sd.skillId(),
                Map.of(
                    "skillId", sd.skillId(),
                    "version", sd.skillVersion() != null ? sd.skillVersion() : "latest",
                    "status", "INVOKED"
                )
            );

            context = new AgentContext(
                context.input(),
                context.goal(),
                context.variables(),
                updatedObs,
                skillResults,
                context.metadata()
            );

            recordStep(iteration, decision, "SKILL", stepStart);
            return null; // continue loop
        }

        // Unknown decision type — should not happen due to sealed interface
        stopCondition = StopCondition.of(StopCondition.StopReason.FAILED, "Unknown decision type");
        return buildResult(AgentResultStatus.FAILED, Map.of(), Map.of(
            "errorCode", "UNKNOWN_DECISION",
            "errorMessage", "Unknown decision type: " + decision.getClass().getName()
        ));
    }

    private void recordStep(long iteration, AgentDecision decision, String phase, Instant start) {
        steps.add(new LoopStep(
            iteration,
            phase,
            decision.getClass().getSimpleName(),
            start,
            Instant.now(),
            Map.of()
        ));
    }

    private AgentResult buildResult(AgentResultStatus status, Map<String, Object> outputs) {
        return buildResult(status, outputs, Map.of());
    }

    private AgentResult buildResult(AgentResultStatus status, Map<String, Object> outputs, Map<String, Object> extraMetadata) {
        Map<String, Object> metadata = new java.util.HashMap<>();
        metadata.put("iterations", budgetTracker.getIterations());
        metadata.put("toolCalls", budgetTracker.getToolCallsMade());
        metadata.put("modelCalls", budgetTracker.getModelCallsMade());
        metadata.put("tokensUsed", budgetTracker.getTokensUsed());
        metadata.put("elapsedMillis", budgetTracker.getElapsed().toMillis());
        if (stopCondition != null) {
            metadata.put("stopReason", stopCondition.reason().name());
        }
        metadata.putAll(extraMetadata);
        return new AgentResult(status, outputs, metadata);
    }

    public AgentContext getContext() {
        return context;
    }

    public StopCondition getStopCondition() {
        return stopCondition;
    }

    public List<LoopStep> getSteps() {
        return List.copyOf(steps);
    }

    public BudgetTracker getBudgetTracker() {
        return budgetTracker;
    }

    // G3-3: convert capability result to map for context
    private static Map<String, Object> resultToMap(
        CapabilityResult result
    ) {
        if (result instanceof CapabilitySuccess s) {
            Map<String, Object> m = new HashMap<>();
            m.put("status", "SUCCESS");
            m.putAll(s.output());
            return Map.copyOf(m);
        }
        if (result instanceof CapabilityFailure f) {
            return Map.of(
                "status", "FAILURE",
                "code", f.code(),
                "message", f.message(),
                "retryable", f.retryable()
            );
        }
        if (result instanceof CapabilityWaiting w) {
            return Map.of(
                "status", "WAITING",
                "reason", w.reason() != null ? w.reason() : ""
            );
        }
        if (result instanceof CapabilityCancelled c) {
            return Map.of(
                "status", "CANCELLED",
                "reason", c.reason() != null ? c.reason() : ""
            );
        }
        return Map.of("status", "UNKNOWN");
    }
}
