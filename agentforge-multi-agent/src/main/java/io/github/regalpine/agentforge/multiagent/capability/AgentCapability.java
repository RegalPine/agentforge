package io.github.regalpine.agentforge.multiagent.capability;

import io.github.regalpine.agentforge.capability.*;
import io.github.regalpine.agentforge.kernel.EntityId;
import io.github.regalpine.agentforge.kernel.Reference;
import io.github.regalpine.agentforge.kernel.Version;
import io.github.regalpine.agentforge.multiagent.contract.AgentContract;
import io.github.regalpine.agentforge.multiagent.delegation.AgentDelegator;
import io.github.regalpine.agentforge.multiagent.delegation.DelegationContext;
import io.github.regalpine.agentforge.multiagent.result.AgentResult;
import io.github.regalpine.agentforge.multiagent.result.ResultStatus;

import java.util.Map;
import java.util.Objects;

/**
 * AgentCapability — AE-11 §14-16.
 *
 * Wraps an agent as a Capability so it enters the
 * standard Invocation Pipeline (AE-02 §24).
 *
 * <p>Agent-to-Agent = AgentCapability + Nested Execution.
 * No second invocation system is created.
 */
public final class AgentCapability
    implements CapabilityProvider {

    private final AgentContract contract;
    private final AgentDelegator delegator;
    private final DelegationContext baseContext;

    public AgentCapability(
        AgentContract contract,
        AgentDelegator delegator,
        DelegationContext baseContext
    ) {
        this.contract = Objects.requireNonNull(contract);
        this.delegator = Objects.requireNonNull(delegator);
        this.baseContext = Objects.requireNonNull(baseContext);
    }

    @Override
    public ProviderDescriptor descriptor() {
        return new ProviderDescriptor(
            "agent-capability",
            new Version("1.0.0"),
            java.util.Set.of(
                contract.toReference()
            ),
            Map.of(
                "type", "agent",
                "agentId", contract.agentId().value(),
                "version", contract.version().value()
            )
        );
    }

    @Override
    public CapabilityResult invoke(
        CapabilityInvocation invocation
    ) {
        Reference agentRef = contract.toReference();

        AgentResult<Map<String, Object>> result =
            delegator.delegate(
                agentRef,
                invocation.arguments(),
                baseContext
            );

        return toCapabilityResult(result);
    }

    /**
     * Get the underlying agent contract.
     */
    public AgentContract agentContract() {
        return contract;
    }

    private CapabilityResult toCapabilityResult(
        AgentResult<Map<String, Object>> result
    ) {
        if (result == null) {
            return new CapabilityFailure(
                "NULL_RESULT",
                "Agent returned null result",
                true
            );
        }

        return switch (result.status()) {
            case SUCCESS -> new CapabilitySuccess(
                result.output() != null
                    ? result.output() : Map.of()
            );
            case PARTIAL -> new CapabilitySuccess(
                result.output() != null
                    ? result.output() : Map.of()
            );
            case POLICY_DENIED -> new CapabilityFailure(
                "POLICY_DENIED",
                result.errorMessage(),
                false
            );
            case BUDGET_EXCEEDED -> new CapabilityFailure(
                "BUDGET_EXCEEDED",
                result.errorMessage(),
                false
            );
            case TIMEOUT -> new CapabilityFailure(
                "TIMEOUT",
                result.errorMessage(),
                true
            );
            case CANCELLED -> new CapabilityCancelled(
                result.errorMessage() != null
                    ? result.errorMessage() : "Cancelled"
            );
            case WAITING_HUMAN -> new CapabilityWaiting(
                result.errorMessage() != null
                    ? result.errorMessage()
                    : "Waiting for human"
            );
            case FAILED -> new CapabilityFailure(
                result.errorCode() != null
                    ? result.errorCode() : "FAILED",
                result.errorMessage(),
                true
            );
        };
    }
}
