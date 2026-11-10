package io.github.regalpine.agentforge.sdk.bootstrap;

import java.util.*;

/**
 * Dependency graph for circular
 * dependency detection.
 * AF-ENHANCE-01 §20: Dependency resolution.
 *
 * <p>Supports adding nodes and directed
 * edges, then detecting cycles via DFS.
 */
public final class DependencyGraph {

    private final Map<String, Set<String>>
        adjacency = new LinkedHashMap<>();

    /**
     * Add a node to the graph.
     */
    public void addNode(String node) {
        adjacency.putIfAbsent(
            node, new LinkedHashSet<>()
        );
    }

    /**
     * Add a directed edge:
     * from depends on to.
     */
    public void addEdge(
        String from, String to
    ) {
        addNode(from);
        addNode(to);
        adjacency.get(from).add(to);
    }

    /**
     * Detect circular dependencies.
     *
     * @return a list of nodes forming
     *   a cycle, or empty if no cycle
     */
    public List<String> detectCycle() {
        Set<String> visited = new HashSet<>();
        Set<String> inStack = new HashSet<>();

        for (String node : adjacency.keySet()) {
            if (!visited.contains(node)) {
                List<String> cycle = dfs(
                    node, visited, inStack,
                    new ArrayList<>()
                );
                if (!cycle.isEmpty()) {
                    return cycle;
                }
            }
        }

        return List.of();
    }

    /**
     * Check if the graph has any
     * circular dependencies.
     */
    public boolean hasCycle() {
        return !detectCycle().isEmpty();
    }

    /**
     * Validate that no circular
     * dependencies exist.
     *
     * @throws BootstrapException if a
     *   cycle is detected
     */
    public void validate() {
        List<String> cycle = detectCycle();
        if (!cycle.isEmpty()) {
            throw new BootstrapException(
                "Circular dependency detected: "
                    + String.join(" → ", cycle)
                    + " → " + cycle.get(0)
            );
        }
    }

    /**
     * Get all nodes in the graph.
     */
    public Set<String> nodes() {
        return Set.copyOf(adjacency.keySet());
    }

    /**
     * Get dependencies of a node.
     */
    public Set<String> dependencies(
        String node
    ) {
        Set<String> deps = adjacency.get(node);
        return deps != null
            ? Set.copyOf(deps) : Set.of();
    }

    private List<String> dfs(
        String node,
        Set<String> visited,
        Set<String> inStack,
        List<String> path
    ) {
        if (inStack.contains(node)) {
            // Found a cycle — extract it
            int idx = path.indexOf(node);
            if (idx >= 0) {
                return new ArrayList<>(
                    path.subList(idx, path.size())
                );
            }
            return List.of(node);
        }

        if (visited.contains(node)) {
            return List.of();
        }

        visited.add(node);
        inStack.add(node);
        path.add(node);

        Set<String> neighbors =
            adjacency.getOrDefault(
                node, Set.of()
            );

        for (String next : neighbors) {
            List<String> cycle = dfs(
                next, visited, inStack, path
            );
            if (!cycle.isEmpty()) {
                return cycle;
            }
        }

        inStack.remove(node);
        path.remove(path.size() - 1);
        return List.of();
    }
}
