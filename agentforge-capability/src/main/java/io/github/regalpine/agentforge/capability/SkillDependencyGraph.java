package io.github.regalpine.agentforge.capability;

import io.github.regalpine.agentforge.kernel.SkillDependencies;
import io.github.regalpine.agentforge.kernel.SkillId;
import io.github.regalpine.agentforge.kernel.SkillReference;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Skill 依赖图与 Cycle Detection.
 * AF-SKILL-02 §26-§28: Dependency Graph.
 * ASM-SKILL-01 C004: 不允许循环依赖.
 *
 * <p>Builds a directed graph G=(V,E) of
 * Skill dependencies and detects cycles
 * using DFS. Also supports version
 * conflict detection.
 *
 * <p>Usage:
 * <pre>
 * SkillDependencyGraph graph =
 *     SkillDependencyGraph.builder()
 *         .addSkill(id, deps)
 *         .addSkill(id2, deps2)
 *         .build();
 *
 * graph.validate(); // throws if cycle
 * </pre>
 */
public class SkillDependencyGraph {

    private final Map<SkillId, Set<SkillId>>
        adjacency;

    private final Map<SkillId, SkillDependencies>
        dependencyMap;

    private SkillDependencyGraph(
        Map<SkillId, Set<SkillId>> adjacency,
        Map<SkillId, SkillDependencies> depMap
    ) {
        this.adjacency = Map.copyOf(adjacency);
        this.dependencyMap =
            Map.copyOf(depMap);
    }

    /**
     * Validate: no cycles exist.
     *
     * @throws IllegalStateException
     *   if a cycle is detected
     */
    public void validate() {
        List<SkillId> cycle = detectCycle();
        if (!cycle.isEmpty()) {
            throw new IllegalStateException(
                "Skill dependency cycle: "
                    + formatCycle(cycle)
            );
        }
    }

    /**
     * Detect cycle using DFS.
     *
     * @return empty list if no cycle,
     *   otherwise the cycle path
     */
    public List<SkillId> detectCycle() {
        Set<SkillId> visited = new HashSet<>();
        Set<SkillId> onStack = new HashSet<>();
        Deque<SkillId> path = new ArrayDeque<>();

        for (SkillId node : adjacency.keySet()) {
            if (!visited.contains(node)) {
                List<SkillId> cycle = dfs(
                    node, visited, onStack,
                    path
                );
                if (!cycle.isEmpty()) {
                    return cycle;
                }
            }
        }

        return List.of();
    }

    private List<SkillId> dfs(
        SkillId node,
        Set<SkillId> visited,
        Set<SkillId> onStack,
        Deque<SkillId> path
    ) {
        visited.add(node);
        onStack.add(node);
        path.push(node);

        Set<SkillId> neighbors =
            adjacency.getOrDefault(
                node, Set.of()
            );

        for (SkillId next : neighbors) {
            if (onStack.contains(next)) {
                // cycle found — extract it
                List<SkillId> cycle =
                    new ArrayList<>();
                cycle.add(next);
                for (SkillId n : path) {
                    cycle.add(n);
                    if (n.equals(next)
                        && cycle.size() > 1) {
                        break;
                    }
                }
                return cycle;
            }

            if (!visited.contains(next)) {
                List<SkillId> cycle = dfs(
                    next, visited, onStack,
                    path
                );
                if (!cycle.isEmpty()) {
                    return cycle;
                }
            }
        }

        path.pop();
        onStack.remove(node);
        return List.of();
    }

    /**
     * Get all dependencies of a Skill.
     */
    public Set<SkillId> dependenciesOf(
        SkillId skillId
    ) {
        return adjacency.getOrDefault(
            skillId, Set.of()
        );
    }

    /**
     * Get transitive dependencies (BFS).
     */
    public Set<SkillId> transitiveDeps(
        SkillId skillId
    ) {
        Set<SkillId> result = new HashSet<>();
        Deque<SkillId> queue = new ArrayDeque<>();
        queue.add(skillId);

        while (!queue.isEmpty()) {
            SkillId current = queue.poll();
            for (SkillId dep :
                adjacency.getOrDefault(
                    current, Set.of()
                )
            ) {
                if (result.add(dep)) {
                    queue.add(dep);
                }
            }
        }

        return Set.copyOf(result);
    }

    /**
     * @return topological order if acyclic.
     * @throws IllegalStateException
     *   if cycle detected
     */
    public List<SkillId> topologicalOrder() {
        validate();

        // adjacency[A]={B,C} means A
        // depends on B and C.
        // For topological order, deps
        // come first. Build reverse map.
        Map<SkillId, Set<SkillId>> reverse =
            new HashMap<>();
        Map<SkillId, Integer> inDegree =
            new HashMap<>();

        for (SkillId node :
            adjacency.keySet()
        ) {
            reverse.putIfAbsent(
                node, new HashSet<>()
            );
            inDegree.putIfAbsent(node, 0);
        }

        for (var e : adjacency.entrySet()) {
            SkillId node = e.getKey();
            // node depends on each dep
            for (SkillId dep : e.getValue()) {
                // reverse: dep → node
                reverse.computeIfAbsent(
                    dep, k -> new HashSet<>()
                ).add(node);
                // node has one more dep
                inDegree.merge(
                    node, 1, Integer::sum
                );
            }
        }

        Deque<SkillId> queue =
            new ArrayDeque<>();
        for (var e : inDegree.entrySet()) {
            if (e.getValue() == 0) {
                queue.add(e.getKey());
            }
        }

        List<SkillId> order = new ArrayList<>();
        while (!queue.isEmpty()) {
            SkillId node = queue.poll();
            order.add(node);
            // for each node that depends
            // on 'node'
            for (SkillId dependent :
                reverse.getOrDefault(
                    node, Set.of()
                )
            ) {
                int d = inDegree.merge(
                    dependent, -1,
                    Integer::sum
                );
                if (d == 0) {
                    queue.add(dependent);
                }
            }
        }

        return List.copyOf(order);
    }

    private static String formatCycle(
        List<SkillId> cycle
    ) {
        StringBuilder sb =
            new StringBuilder();
        for (int i = 0; i < cycle.size(); i++) {
            if (i > 0) sb.append(" → ");
            sb.append(cycle.get(i).value());
        }
        return sb.toString();
    }

    /**
     * Create a new builder.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for SkillDependencyGraph.
     */
    public static class Builder {

        private final Map<SkillId, Set<SkillId>>
            adj = new HashMap<>();

        private final Map<SkillId, SkillDependencies>
            depMap = new HashMap<>();

        /**
         * Add a Skill with its dependencies.
         */
        public Builder addSkill(
            SkillId id,
            SkillDependencies deps
        ) {
            Objects.requireNonNull(id);
            Objects.requireNonNull(deps);

            adj.putIfAbsent(id, new HashSet<>());
            depMap.put(id, deps);

            // add Skill → Skill edges
            for (SkillReference ref :
                deps.skills()
            ) {
                SkillId depId = ref.skillId();
                adj.get(id).add(depId);
                adj.putIfAbsent(
                    depId, new HashSet<>()
                );
            }

            return this;
        }

        public SkillDependencyGraph build() {
            return new SkillDependencyGraph(
                adj, depMap
            );
        }
    }
}
