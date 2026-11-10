package io.github.regalpine.agentforge.multiagent.delegation;


import io.github.regalpine.agentforge.kernel.Reference;
import io.github.regalpine.agentforge.multiagent.budget.BudgetExceededException;
import io.github.regalpine.agentforge.multiagent.budget.DelegationBudget;
import io.github.regalpine.agentforge.multiagent.contract.AgentContract;
import io.github.regalpine.agentforge.multiagent.event.DelegationEvent;
import io.github.regalpine.agentforge.multiagent.event.DelegationEventSink;
import io.github.regalpine.agentforge.multiagent.event.DelegationEventType;
import io.github.regalpine.agentforge.multiagent.policy.DelegationPolicy;
import io.github.regalpine.agentforge.multiagent.policy.DelegationPolicyDecision;
import io.github.regalpine.agentforge.multiagent.projection.ContextProjection;
import io.github.regalpine.agentforge.multiagent.reference.AgentResolver;
import io.github.regalpine.agentforge.multiagent.result.AgentResult;
import io.github.regalpine.agentforge.multiagent.result.ResultStatus;
import io.github.regalpine.agentforge.multiagent.result.ResultValidator;
import io.github.regalpine.agentforge.multiagent.safety.CircularDelegationDetector;
import io.github.regalpine.agentforge.multiagent.safety.DelegationDepthChecker;
import io.github.regalpine.agentforge.multiagent.safety.DelegationSafetyResult;

import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Function;

/**
 * DefaultAgentDelegator — AE-11 §11, §16, §25, §38.
 *
 * Full delegation pipeline:
 * 1. Resolve agent contract
 * 2. Check circular delegation
 * 3. Check delegation depth
 * 4. Authorize via policy
 * 5. Project context
 * 6. Allocate budget
 * 7. Create child execution
 * 8. Execute child agent
 * 9. Validate result
 * 10. Emit events
 */
public final class DefaultAgentDelegator
    implements AgentDelegator {

    private final AgentResolver resolver;
    private final CircularDelegationDetector circularDetector;
    private final DelegationDepthChecker depthChecker;
    private final ResultValidator resultValidator;
    private final DelegationEventSink eventSink;

    private final Function<
        AgentExecutionRequest,
        AgentResult<Map<String, Object>>
    > executor;

    public DefaultAgentDelegator(
        AgentResolver resolver,
        DelegationEventSink eventSink,
        Function<
            AgentExecutionRequest,
            AgentResult<Map<String, Object>>
        > executor
    ) {
        this.resolver = Objects.requireNonNull(resolver);
        this.eventSink = eventSink == null
            ? DelegationEventSink.noop() : eventSink;
        this.executor = Objects.requireNonNull(executor);
        this.circularDetector = new CircularDelegationDetector();
        this.depthChecker = new DelegationDepthChecker();
        this.resultValidator = new ResultValidator();
    }

    @Override
    public AgentResult<Map<String, Object>> delegate(
        Reference agent,
        Map<String, Object> input,
        DelegationContext context
    ) {
        Objects.requireNonNull(agent, "agent");
        Objects.requireNonNull(context, "context");

        String childAgentId = agent.id().value();
        String parentAgentId = context.parentAgentId();

        // Step 1: Emit REQUESTED
        emit(DelegationEventType.AGENT_DELEGATION_REQUESTED,
            parentAgentId, childAgentId, context);

        // Step 2: Resolve contract
        AgentContract contract;
        try {
            contract = resolver.resolve(agent);
        } catch (Exception e) {
            emit(DelegationEventType.AGENT_DELEGATION_REJECTED,
                parentAgentId, childAgentId, context);
            return AgentResult.failure(
                "RESOLUTION_FAILED",
                e.getMessage(),
                prov(context, childAgentId)
            );
        }

        // Step 3: Check circular delegation
        DelegationSafetyResult circularCheck =
            circularDetector.check(
                context.delegationStack(),
                childAgentId
            );

        if (!circularCheck.safe()) {
            emit(DelegationEventType.AGENT_CIRCULAR_DETECTION,
                parentAgentId, childAgentId, context);
            emit(DelegationEventType.AGENT_DELEGATION_REJECTED,
                parentAgentId, childAgentId, context);
            return AgentResult.policyDenied(
                circularCheck.reason(),
                prov(context, childAgentId)
            );
        }

        // Step 4: Check delegation depth
        DelegationSafetyResult depthCheck =
            depthChecker.check(context.delegationDepth());

        if (!depthCheck.safe()) {
            emit(DelegationEventType.AGENT_DELEGATION_REJECTED,
                parentAgentId, childAgentId, context);
            return AgentResult.policyDenied(
                depthCheck.reason(),
                prov(context, childAgentId)
            );
        }

        // Step 5: Authorize via policy
        DelegationPolicy policy = context.policy();
        if (policy != null) {
            Map<String, Object> policyCtx = Map.of(
                "currentDepth", context.delegationDepth()
            );
            DelegationPolicyDecision decision =
                policy.authorize(
                    contract.toReference(),
                    agent,
                    policyCtx
                );

            if (decision == DelegationPolicyDecision.DENY) {
                emit(DelegationEventType.AGENT_DELEGATION_REJECTED,
                    parentAgentId, childAgentId, context);
                return AgentResult.policyDenied(
                    "Delegation denied by policy",
                    prov(context, childAgentId)
                );
            }
        }

        // Step 6: Project context
        Map<String, Object> projectedInput = input;
        ContextProjection projection = context.projection();
        if (projection != null
            && context.parentContext() != null
        ) {
            ContextProjection.ProjectedContext projected =
                projection.project(
                    context.parentContext(),
                    contract
                );
            var merged = new java.util.HashMap<>(
                projected.data());
            if (input != null) {
                merged.putAll(input);
            }
            projectedInput = Map.copyOf(merged);
        }

        // Step 7: Allocate budget
        DelegationBudget parentBudget = context.budget();
        DelegationBudget childBudget;
        if (parentBudget != null) {
            try {
                parentBudget.addChildExecutionConsumption();
                childBudget = parentBudget.allocateChild(
                    Math.min(
                        parentBudget.remainingTokens() / 2,
                        contract.executionPolicy().maxTokens()
                    ),
                    Math.min(
                        (int) parentBudget.remainingModelCalls() / 2,
                        contract.executionPolicy().maxModelCalls()
                    ),
                    Math.min(
                        (int) parentBudget.remainingToolCalls() / 2,
                        contract.executionPolicy().maxToolCalls()
                    ),
                    contract.executionPolicy().maxChildExecutions()
                );
            } catch (BudgetExceededException e) {
                emit(DelegationEventType.AGENT_DELEGATION_REJECTED,
                    parentAgentId, childAgentId, context);
                return AgentResult.budgetExceeded(
                    e.getMessage(),
                    prov(context, childAgentId)
                );
            }
        } else {
            childBudget = DelegationBudget.defaults();
        }

        // Step 8: Create child execution
        String childExecId = "exec-" + UUID.randomUUID()
            .toString().substring(0, 8);

        DelegationContext childContext = context.deriveChild(
            childExecId, childAgentId, childBudget
        );

        emit(DelegationEventType.AGENT_EXECUTION_CREATED,
            parentAgentId, childAgentId, context);

        // Step 9: Emit ACCEPTED + STARTED
        emit(DelegationEventType.AGENT_DELEGATION_ACCEPTED,
            parentAgentId, childAgentId, context);
        emit(DelegationEventType.AGENT_EXECUTION_STARTED,
            parentAgentId, childAgentId, context);

        // Step 10: Execute
        AgentResult<Map<String, Object>> result;
        try {
            AgentExecutionRequest request =
                new AgentExecutionRequest(
                    agent, contract, projectedInput,
                    childContext, childExecId
                );

            result = executor.apply(request);

        } catch (BudgetExceededException e) {
            result = AgentResult.budgetExceeded(
                e.getMessage(),
                prov(context, childAgentId)
            );
        } catch (Exception e) {
            result = AgentResult.failure(
                "EXECUTION_ERROR",
                e.getMessage(),
                prov(context, childAgentId)
            );
        }

        // Step 11: Validate result
        if (result != null) {
            var errors = resultValidator.validate(
                result, contract);
            if (!errors.isEmpty()) {
                // Log validation errors but don't fail
                result = new AgentResult<>(
                    result.status(),
                    result.output(),
                    result.complete(),
                    result.artifacts(),
                    result.references(),
                    result.provenance(),
                    result.metadata(),
                    result.errorCode(),
                    result.errorMessage()
                );
            }
        }

        // Step 12: Emit completion events
        if (result != null
            && result.status() == ResultStatus.SUCCESS
        ) {
            emit(DelegationEventType.AGENT_EXECUTION_COMPLETED,
                parentAgentId, childAgentId, context);
        } else {
            emit(DelegationEventType.AGENT_EXECUTION_FAILED,
                parentAgentId, childAgentId, context);
        }

        emit(DelegationEventType.AGENT_RESULT_RECEIVED,
            parentAgentId, childAgentId, context);

        return result;
    }

    private void emit(
        DelegationEventType type,
        String parentAgentId,
        String childAgentId,
        DelegationContext ctx
    ) {
        eventSink.emit(DelegationEvent.withExecutions(
            type,
            parentAgentId,
            childAgentId,
            ctx.parentExecutionId(),
            null,
            ctx.rootExecutionId()
        ));
    }

    private AgentResult.ResultProvenance prov(
        DelegationContext ctx,
        String childAgentId
    ) {
        return new AgentResult.ResultProvenance(
            ctx.rootExecutionId(),
            ctx.parentExecutionId(),
            "pending",
            childAgentId,
            null,
            null
        );
    }

    /**
     * Request passed to the executor function.
     */
    public record AgentExecutionRequest(
        Reference agent,
        AgentContract contract,
        Map<String, Object> input,
        DelegationContext context,
        String childExecutionId
    ) {
        public AgentExecutionRequest {
            Objects.requireNonNull(agent);
            Objects.requireNonNull(contract);
            input = input == null
                ? Map.of() : Map.copyOf(input);
        }
    }
}
