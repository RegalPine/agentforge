package io.github.regalpine.agentforge.graph.ir;

import io.github.regalpine.agentforge.graph.EdgeType;
import io.github.regalpine.agentforge.kernel.EntityId;

/**
 * A routing entry in the execution plan.
 * <p>
 * G3-1: Preserves edge semantics (type, condition)
 * in the execution plan routing map, so the runtime
 * can evaluate conditions, follow error edges, and
 * handle loop-back edges correctly.
 *
 * @param target    the target node ID
 * @param edgeType  the edge type (NORMAL, CONDITIONAL, ERROR, LOOP_BACK)
 * @param condition the condition expression (may be null)
 */
public record RoutingEntry(
    EntityId target,
    EdgeType edgeType,
    String condition
) {

    public RoutingEntry {
        if (edgeType == null) {
            edgeType = EdgeType.NORMAL;
        }
    }

    /**
     * Convenience factory for a NORMAL edge.
     */
    public static RoutingEntry normal(EntityId target) {
        return new RoutingEntry(target, EdgeType.NORMAL, null);
    }

    /**
     * Convenience factory for an ERROR edge.
     */
    public static RoutingEntry error(EntityId target) {
        return new RoutingEntry(target, EdgeType.ERROR, null);
    }

    /**
     * Convenience factory for a CONDITIONAL edge.
     */
    public static RoutingEntry conditional(
        EntityId target, String condition
    ) {
        return new RoutingEntry(
            target, EdgeType.CONDITIONAL, condition
        );
    }

    /**
     * Convenience factory for a LOOP_BACK edge.
     */
    public static RoutingEntry loopBack(EntityId target) {
        return new RoutingEntry(
            target, EdgeType.LOOP_BACK, null
        );
    }
}
