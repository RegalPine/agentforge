package io.github.regalpine.agentforge.runtime;

import io.github.regalpine.agentforge.graph.EdgeType;
import io.github.regalpine.agentforge.graph.ir.ExecutionPlan;
import io.github.regalpine.agentforge.graph.ir.RoutingEntry;
import io.github.regalpine.agentforge.kernel.EntityId;
import io.github.regalpine.agentforge.runtime.condition.ConditionEvaluator;

import java.util.List;
import java.util.Map;

/**
 * Routes execution to the next node
 * based on edge type and result.
 * <p>
 * G3-1: Edge semantics are now preserved
 * in the routing table. This router evaluates
 * edge types to determine the correct next node:
 * <ul>
 *   <li>NORMAL — always followed on success</li>
 *   <li>CONDITIONAL — followed if condition matches</li>
 *   <li>ERROR — followed only on node failure</li>
 *   <li>LOOP_BACK — followed for iteration</li>
 * </ul>
 */
public final class ExecutionRouter {

    /**
     * Get all routing entries for a node.
     */
    public List<RoutingEntry> routes(
        ExecutionPlan plan,
        EntityId nodeId
    ) {
        return plan.routing()
            .getOrDefault(
                nodeId,
                List.of()
            );
    }

    /**
     * Resolve the next node after a successful
     * node execution.
     * <p>
     * Priority: CONDITIONAL (matching) > NORMAL >
     * LOOP_BACK.
     * ERROR edges are skipped on success.
     *
     * @param plan      the execution plan
     * @param nodeId    the current node
     * @param stateData the current execution state
     *                  data (for condition evaluation)
     * @return the next node ID, or null if none
     */
    public EntityId nextOnSuccess(
        ExecutionPlan plan,
        EntityId nodeId,
        Map<String, Object> stateData
    ) {
        List<RoutingEntry> entries = routes(plan, nodeId);

        // First try CONDITIONAL edges
        for (RoutingEntry entry : entries) {
            if (entry.edgeType() == EdgeType.CONDITIONAL) {
                if (evaluateCondition(
                    entry.condition(), stateData
                )) {
                    return entry.target();
                }
            }
        }

        // Then try NORMAL edges
        for (RoutingEntry entry : entries) {
            if (entry.edgeType() == EdgeType.NORMAL) {
                return entry.target();
            }
        }

        // Then try LOOP_BACK edges
        for (RoutingEntry entry : entries) {
            if (entry.edgeType() == EdgeType.LOOP_BACK) {
                return entry.target();
            }
        }

        // No matching edge found
        return null;
    }

    /**
     * Resolve the next node after a failed
     * node execution.
     * <p>
     * Only ERROR edges are followed on failure.
     *
     * @param plan   the execution plan
     * @param nodeId the current node
     * @return the next node ID, or null if none
     */
    public EntityId nextOnFailure(
        ExecutionPlan plan,
        EntityId nodeId
    ) {
        List<RoutingEntry> entries = routes(plan, nodeId);

        for (RoutingEntry entry : entries) {
            if (entry.edgeType() == EdgeType.ERROR) {
                return entry.target();
            }
        }

        return null;
    }

    /**
     * Backward-compatible next() method.
     * Returns the first NORMAL/any target.
     */
    public EntityId next(
        ExecutionPlan plan,
        EntityId nodeId
    ) {
        List<RoutingEntry> entries = routes(plan, nodeId);
        if (entries.isEmpty()) {
            return null;
        }
        // Prefer NORMAL edges
        for (RoutingEntry entry : entries) {
            if (entry.edgeType() == EdgeType.NORMAL) {
                return entry.target();
            }
        }
        // Fall back to first entry
        return entries.get(0).target();
    }

    /**
     * Evaluate a condition expression against
     * the current state data.
     * <p>
     * Delegates to {@link ConditionEvaluator} which
     * supports path expressions, comparisons, logical
     * operators and parentheses. Backward compatible
     * with simple key-based truthy checks.
     */
    private boolean evaluateCondition(
        String condition,
        Map<String, Object> stateData
    ) {
        return ConditionEvaluator.evaluate(condition, stateData);
    }
}
