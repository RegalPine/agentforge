package io.github.regalpine.agentforge.provider.agent;

import io.github.regalpine.agentforge.capability.*;
import io.github.regalpine.agentforge.kernel.EntityId;
import io.github.regalpine.agentforge.kernel.Reference;
import io.github.regalpine.agentforge.kernel.Version;

import java.util.Map;
import java.util.Set;
import java.util.function.Function;

public final class AgentProvider
    implements CapabilityProvider {

    private final Function<
        AgentInvocation,
        CapabilityResult
    > agentExecutor;

    public AgentProvider(
        Function<
            AgentInvocation,
            CapabilityResult
        > agentExecutor
    ) {
        this.agentExecutor = agentExecutor;
    }

    @Override
    public ProviderDescriptor descriptor() {
        return new ProviderDescriptor(
            "agent",
            new Version("1.0.0"),
            Set.of(
                new Reference(
                    new EntityId("agent.invoke"),
                    new Version("1.0.0")
                )
            ),
            Map.of("type", "reference")
        );
    }

    @Override
    public CapabilityResult invoke(
        CapabilityInvocation invocation
    ) {
        Object agentIdObj =
            invocation.arguments()
                .get("agentId");

        Object agentVersionObj =
            invocation.arguments()
                .get("agentVersion");

        if (agentIdObj == null) {
            return new CapabilityFailure(
                "AF-AGENT-001",
                "Missing 'agentId' argument",
                false
            );
        }

        String agentId = agentIdObj.toString();

        String agentVersion =
            agentVersionObj != null
                ? agentVersionObj.toString()
                : "1.0.0";

        String rootExecId =
            invocation.context() != null
                ? invocation.context()
                    .executionId()
                : "unknown";

        int depth = 0;

        if (invocation.context() != null
            && invocation.context()
                .attributes()
                .containsKey("depth")
        ) {
            Object depthObj =
                invocation.context()
                    .attributes()
                    .get("depth");

            if (depthObj instanceof Number num) {
                depth = num.intValue();
            }
        }

        int maxDepth =
            AgentCallContext.DEFAULT_MAX_DEPTH;

        if (depth >= maxDepth) {
            return new CapabilityFailure(
                "AF-AGENT-002",
                "Agent recursion limit exceeded: "
                    + maxDepth,
                false
            );
        }

        AgentCallContext callCtx =
            new AgentCallContext(
                rootExecId,
                depth,
                maxDepth
            );

        AgentInvocation agentInvocation =
            new AgentInvocation(
                new Reference(
                    new EntityId(agentId),
                    new Version(agentVersion)
                ),
                invocation.arguments(),
                callCtx
            );

        return agentExecutor.apply(agentInvocation);
    }
}
