package io.github.regalpine.agentforge.runtime.agent;

import io.github.regalpine.agentforge.agent.ActionExecutionPort;
import io.github.regalpine.agentforge.agent.AgentContext;
import io.github.regalpine.agentforge.agent.AgentDecision;
import io.github.regalpine.agentforge.agent.AgentDecisionContext;
import io.github.regalpine.agentforge.agent.AgentDecisionEngine;
import io.github.regalpine.agentforge.agent.AgentObservation;
import io.github.regalpine.agentforge.agent.AgentResult;
import io.github.regalpine.agentforge.agent.AgentResultStatus;
import io.github.regalpine.agentforge.agent.AgentRuntimeOptions;
import io.github.regalpine.agentforge.agent.AgentRuntimePolicy;
import io.github.regalpine.agentforge.agent.AgentRuntimeRequest;
import io.github.regalpine.agentforge.agent.CapabilityDecision;
import io.github.regalpine.agentforge.agent.FailDecision;
import io.github.regalpine.agentforge.agent.FinishDecision;
import io.github.regalpine.agentforge.agent.WaitDecision;
import io.github.regalpine.agentforge.capability.*;
import io.github.regalpine.agentforge.graph.NodeType;
import io.github.regalpine.agentforge.graph.ir.ExecutionNode;
import io.github.regalpine.agentforge.runtime.NodeExecutionResult;
import io.github.regalpine.agentforge.runtime.NodeExecutor;
import io.github.regalpine.agentforge.runtime.NodeExecutionContext;
import io.github.regalpine.agentforge.runtime.NodeCompleted;
import io.github.regalpine.agentforge.runtime.NodeFailed;
import io.github.regalpine.agentforge.runtime.NodeWaiting;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * NodeExecutor for AGENT NodeType.
 * AF-05 §3 + AF-06 §3: AgentNodeExecutor.
 *
 * <p>Bridges Graph Runtime with Agent Runtime.
 * When an AGENT node is encountered during graph
 * execution, this executor delegates to the
 * {@link AgentDecisionEngine} for the Agent's
 * autonomous decision loop.
 *
 * <p>Graph Runtime only sees the final
 * {@link AgentResult} — it does not understand
 * the internal decision cycle.
 */
public final class AgentNodeExecutor
    implements NodeExecutor {

    private final AgentDecisionEngine decisionEngine;
    // G3-3: action execution port for real capability invocation
    private final ActionExecutionPort actionPort;

    public AgentNodeExecutor(
        AgentDecisionEngine decisionEngine
    ) {
        this(decisionEngine, ActionExecutionPort.noOp());
    }

    /**
     * G3-3: constructor with action port.
     */
    public AgentNodeExecutor(
        AgentDecisionEngine decisionEngine,
        ActionExecutionPort actionPort
    ) {
        this.decisionEngine = Objects.requireNonNull(
            decisionEngine, "decisionEngine"
        );
        this.actionPort = Objects.requireNonNull(
            actionPort, "actionPort"
        );
    }

    @Override
    public NodeExecutionResult execute(
        ExecutionNode node,
        NodeExecutionContext context
    ) {
        if (node.type() != NodeType.AGENT) {
            return new NodeFailed(
                "AGENT_TYPE_MISMATCH",
                "AgentNodeExecutor can only handle AGENT nodes, got: "
                    + node.type(),
                false
            );
        }

        try {
            AgentRuntimeRequest request = buildRequest(node, context);
            AgentResult result = executeAgentLoop(request, context);
            return toNodeResult(result);
        } catch (Exception e) {
            return new NodeFailed(
                "AGENT_EXECUTION_ERROR",
                e.getMessage(),
                true
            );
        }
    }

    private AgentRuntimeRequest buildRequest(
        ExecutionNode node,
        NodeExecutionContext context
    ) {
        Map<String, Object> config = node.config();
        String agentId = (String) config.getOrDefault(
            "agentId",
            node.id().value()
        );
        String agentVersion = (String) config.getOrDefault(
            "agentVersion",
            "1.0.0"
        );

        return new AgentRuntimeRequest(
            agentId,
            agentVersion,
            context.state().data(),
            AgentRuntimeOptions.defaults()
        );
    }

    private AgentResult executeAgentLoop(
        AgentRuntimeRequest request,
        NodeExecutionContext context
    ) {
        AgentContext agentContext = new AgentContext(
            request.inputs(),
            request.inputs().getOrDefault("goal", "").toString(),
            Map.of(),
            java.util.List.of(),
            Map.of(),
            Map.of()
        );

        AgentDecisionContext decisionContext = new AgentDecisionContext(
            null,
            agentContext,
            java.util.List.of(),
            AgentRuntimePolicy.defaults()
        );

        AgentRuntimeOptions options = request.options() != null
            ? request.options()
            : AgentRuntimeOptions.defaults();

        long maxIterations = options.maxIterations();

        for (long i = 0; i < maxIterations; i++) {
            AgentDecision decision = decisionEngine.decide(decisionContext);

            if (decision instanceof FinishDecision fd) {
                return new AgentResult(
                    AgentResultStatus.COMPLETED,
                    fd.outputs(),
                    Map.of()
                );
            } else if (decision instanceof WaitDecision wd) {
                return new AgentResult(
                    AgentResultStatus.WAITING,
                    Map.of("waitKey", wd.waitKey()),
                    Map.of()
                );
            } else if (decision instanceof FailDecision fd) {
                return new AgentResult(
                    AgentResultStatus.FAILED,
                    Map.of(),
                    Map.of(
                        "errorCode", fd.code(),
                        "errorMessage", fd.message()
                    )
                );
            } else if (decision instanceof CapabilityDecision cd) {
                // G3-3: actually invoke the capability
                CapabilityResult capResult =
                    actionPort.execute(cd);

                AgentObservation observation = new AgentObservation(
                    cd.capabilityId() + "-" + i,
                    capResult,
                    java.time.Instant.now()
                );

                java.util.List<AgentObservation> updatedObs =
                    new java.util.ArrayList<>(agentContext.observations());
                updatedObs.add(observation);

                // G3-3: store result in capability results
                Map<String, Object> updatedResults =
                    new HashMap<>(agentContext.capabilityResults());
                updatedResults.put(cd.capabilityId(), resultToMap(capResult));

                agentContext = new AgentContext(
                    agentContext.input(),
                    agentContext.goal(),
                    agentContext.variables(),
                    updatedObs,
                    Map.copyOf(updatedResults),
                    agentContext.metadata()
                );

                // G3-3: handle waiting/cancelled results
                if (capResult instanceof CapabilityWaiting w) {
                    return new AgentResult(
                        AgentResultStatus.WAITING,
                        Map.of("waitKey", w.reason()),
                        Map.of()
                    );
                }
                if (capResult instanceof CapabilityCancelled c) {
                    return new AgentResult(
                        AgentResultStatus.CANCELLED,
                        Map.of(),
                        Map.of("reason", c.reason())
                    );
                }

                decisionContext = new AgentDecisionContext(
                    decisionContext.agent(),
                    agentContext,
                    decisionContext.capabilities(),
                    decisionContext.policy()
                );
            }
        }

        return new AgentResult(
            AgentResultStatus.FAILED,
            Map.of(),
            Map.of(
                "errorCode", "MAX_ITERATIONS_EXCEEDED",
                "errorMessage", "Agent exceeded max iterations: "
                    + maxIterations
            )
        );
    }

    private NodeExecutionResult toNodeResult(
        AgentResult result
    ) {
        return switch (result.status()) {
            case COMPLETED -> new NodeCompleted(result.outputs());
            case WAITING -> new NodeWaiting(
                result.outputs().getOrDefault("waitKey", "agent-waiting").toString()
            );
            case FAILED -> new NodeFailed(
                result.metadata().getOrDefault("errorCode", "AGENT_FAILED").toString(),
                result.metadata().getOrDefault("errorMessage", "Agent execution failed").toString(),
                false
            );
            case CANCELLED -> new NodeFailed(
                "AGENT_CANCELLED",
                "Agent execution was cancelled",
                false
            );
        };
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
