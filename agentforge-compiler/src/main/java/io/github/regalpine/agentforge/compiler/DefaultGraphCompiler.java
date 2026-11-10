package io.github.regalpine.agentforge.compiler;

import io.github.regalpine.agentforge.compiler.internal.ResolvedGraph;
import io.github.regalpine.agentforge.compiler.internal.ResolvedNode;
import io.github.regalpine.agentforge.graph.BasicGraphValidator;
import io.github.regalpine.agentforge.graph.EdgeDefinition;
import io.github.regalpine.agentforge.graph.EdgeType;
import io.github.regalpine.agentforge.graph.GraphDefinition;
import io.github.regalpine.agentforge.graph.NodeDefinition;
import io.github.regalpine.agentforge.graph.NodeType;
import io.github.regalpine.agentforge.graph.ir.ExecutionNode;
import io.github.regalpine.agentforge.graph.ir.ExecutionPlan;
import io.github.regalpine.agentforge.graph.ir.GraphIR;
import io.github.regalpine.agentforge.graph.ir.IREdge;
import io.github.regalpine.agentforge.graph.ir.IRNode;
import io.github.regalpine.agentforge.graph.ir.RoutingEntry;
import io.github.regalpine.agentforge.kernel.EntityId;
import io.github.regalpine.agentforge.kernel.Reference;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

public final class DefaultGraphCompiler
    implements GraphCompiler {

    private static final String COMPILER_VERSION = "1.0.0";

    @Override
    public CompilationResult compile(
        GraphDefinition graph,
        CompilationContext context
    ) {

        List<CompilationDiagnostic> diagnostics =
            new ArrayList<>();

        // Phase 1: Basic Validation
        try {
            new BasicGraphValidator().validate(graph);
        } catch (IllegalArgumentException e) {
            diagnostics.add(
                new CompilationDiagnostic(
                    "AF-C001",
                    CompilationDiagnostic.Severity.ERROR,
                    e.getMessage(),
                    null,
                    null
                )
            );
            return new CompilationResult.Failure(diagnostics);
        }

        // Phase 2: Reference Resolution
        ResolvedGraph resolved =
            resolveReferences(graph, context, diagnostics);

        if (hasErrors(diagnostics)) {
            return new CompilationResult.Failure(diagnostics);
        }

        // Phase 3: Structural Analysis
        analyzeStructure(graph, diagnostics);

        if (hasErrors(diagnostics)) {
            return new CompilationResult.Failure(diagnostics);
        }

        // Phase 4: Cycle Detection
        analyzeCycles(graph, diagnostics);

        if (hasErrors(diagnostics)) {
            return new CompilationResult.Failure(diagnostics);
        }

        // Phase 5: Normalization + IR Generation
        GraphIR graphIR = buildIR(graph, resolved);

        // Phase 6: ExecutionPlan Generation
        ExecutionPlan plan = buildPlan(graph, resolved, graphIR);

        return new CompilationResult.Success(
            graphIR,
            plan
        );
    }

    private ResolvedGraph resolveReferences(
        GraphDefinition graph,
        CompilationContext context,
        List<CompilationDiagnostic> diagnostics
    ) {

        RegistrySnapshot snapshot =
            context.registrySnapshot();

        List<ResolvedNode> resolvedNodes =
            new ArrayList<>();

        for (NodeDefinition node : graph.nodes()) {

            Map<String, Object> resolvedRefs =
                new HashMap<>();

            if (node.type() == NodeType.CAPABILITY
                && node.reference() != null) {

                Reference ref = node.reference();

                if (!snapshot.capabilities().containsKey(ref)) {
                    diagnostics.add(
                        new CompilationDiagnostic(
                            "AF-C002",
                            CompilationDiagnostic.Severity.ERROR,
                            "Missing capability: " + ref,
                            node.id(),
                            null
                        )
                    );
                } else {
                    resolvedRefs.put(
                        "capability",
                        ref.id().value() + "@" + ref.version().value()
                    );
                }
            }

            if (node.type() == NodeType.AGENT
                && node.reference() != null) {

                Reference ref = node.reference();

                if (!snapshot.agents().containsKey(ref)) {
                    diagnostics.add(
                        new CompilationDiagnostic(
                            "AF-C003",
                            CompilationDiagnostic.Severity.ERROR,
                            "Missing agent: " + ref,
                            node.id(),
                            null
                        )
                    );
                } else {
                    resolvedRefs.put(
                        "agent",
                        ref.id().value() + "@" + ref.version().value()
                    );
                }
            }

            resolvedNodes.add(
                new ResolvedNode(node, resolvedRefs)
            );
        }

        return new ResolvedGraph(
            graph,
            resolvedNodes,
            graph.edges()
        );
    }

    private void analyzeStructure(
        GraphDefinition graph,
        List<CompilationDiagnostic> diagnostics
    ) {

        Set<String> nodeIds = new HashSet<>();
        for (NodeDefinition node : graph.nodes()) {
            nodeIds.add(node.id().value());
        }

        // Check edge source/target existence
        for (EdgeDefinition edge : graph.edges()) {
            if (!nodeIds.contains(edge.source().value())) {
                diagnostics.add(
                    new CompilationDiagnostic(
                        "AF-C008",
                        CompilationDiagnostic.Severity.ERROR,
                        "Edge source not found: " + edge.source(),
                        null,
                        edge.id()
                    )
                );
            }
            if (!nodeIds.contains(edge.target().value())) {
                diagnostics.add(
                    new CompilationDiagnostic(
                        "AF-C008",
                        CompilationDiagnostic.Severity.ERROR,
                        "Edge target not found: " + edge.target(),
                        null,
                        edge.id()
                    )
                );
            }
        }

        if (hasErrors(diagnostics)) {
            return;
        }

        // Build adjacency for reachability
        Map<String, List<String>> adjacency =
            new HashMap<>();

        for (EdgeDefinition edge : graph.edges()) {
            adjacency
                .computeIfAbsent(
                    edge.source().value(),
                    k -> new ArrayList<>()
                )
                .add(edge.target().value());
        }

        // Find START and END nodes
        List<String> startNodes = new ArrayList<>();
        List<String> endNodes = new ArrayList<>();

        for (NodeDefinition node : graph.nodes()) {
            if (node.type() == NodeType.START) {
                startNodes.add(node.id().value());
            }
            if (node.type() == NodeType.END) {
                endNodes.add(node.id().value());
            }
        }

        // Forward reachability from START
        Set<String> reachableFromStart =
            bfs(startNodes, adjacency);

        for (String nodeId : nodeIds) {
            if (!reachableFromStart.contains(nodeId)) {
                diagnostics.add(
                    new CompilationDiagnostic(
                        "AF-C004",
                        CompilationDiagnostic.Severity.ERROR,
                        "Unreachable node: " + nodeId,
                        new EntityId(nodeId),
                        null
                    )
                );
            }
        }

        // Check END reachability
        boolean canReachEnd =
            endNodes.stream()
                .anyMatch(reachableFromStart::contains);

        if (!canReachEnd && !endNodes.isEmpty()) {
            diagnostics.add(
                new CompilationDiagnostic(
                    "AF-C005",
                    CompilationDiagnostic.Severity.ERROR,
                    "END is not reachable from START",
                    null,
                    null
                )
            );
        }

        // Reverse reachability from END
        Map<String, List<String>> reverseAdj =
            new HashMap<>();

        for (EdgeDefinition edge : graph.edges()) {
            reverseAdj
                .computeIfAbsent(
                    edge.target().value(),
                    k -> new ArrayList<>()
                )
                .add(edge.source().value());
        }

        Set<String> canReachEndSet =
            bfs(endNodes, reverseAdj);

        for (String nodeId : nodeIds) {
            if (reachableFromStart.contains(nodeId)
                && !canReachEndSet.contains(nodeId)) {

                diagnostics.add(
                    new CompilationDiagnostic(
                        "AF-C006",
                        CompilationDiagnostic.Severity.ERROR,
                        "Node cannot reach END: " + nodeId,
                        new EntityId(nodeId),
                        null
                    )
                );
            }
        }
    }

    private void analyzeCycles(
        GraphDefinition graph,
        List<CompilationDiagnostic> diagnostics
    ) {

        // Build adjacency excluding LOOP_BACK edges
        Map<String, List<String>> nonLoopAdj =
            new HashMap<>();

        for (EdgeDefinition edge : graph.edges()) {
            if (edge.type() != EdgeType.LOOP_BACK) {
                nonLoopAdj
                    .computeIfAbsent(
                        edge.source().value(),
                        k -> new ArrayList<>()
                    )
                    .add(edge.target().value());
            }
        }

        // Detect cycles using DFS coloring
        Set<String> allNodes = new HashSet<>();
        for (NodeDefinition node : graph.nodes()) {
            allNodes.add(node.id().value());
        }

        // 0 = white (unvisited), 1 = gray (in progress), 2 = black (done)
        Map<String, Integer> color = new HashMap<>();
        for (String n : allNodes) {
            color.put(n, 0);
        }

        for (String node : allNodes) {
            if (color.get(node) == 0) {
                if (hasCycleDFS(
                    node, nonLoopAdj, color, new HashSet<>()
                )) {
                    diagnostics.add(
                        new CompilationDiagnostic(
                            "AF-C007",
                            CompilationDiagnostic.Severity.ERROR,
                            "Implicit cycle detected among non-LOOP_BACK edges",
                            null,
                            null
                        )
                    );
                    return;
                }
            }
        }
    }

    private boolean hasCycleDFS(
        String node,
        Map<String, List<String>> adj,
        Map<String, Integer> color,
        Set<String> recursionStack
    ) {

        color.put(node, 1);
        recursionStack.add(node);

        List<String> neighbors =
            adj.getOrDefault(node, List.of());

        for (String neighbor : neighbors) {
            if (recursionStack.contains(neighbor)) {
                return true;
            }

            if (color.get(neighbor) == 0) {
                if (hasCycleDFS(
                    neighbor, adj, color, recursionStack
                )) {
                    return true;
                }
            }
        }

        recursionStack.remove(node);
        color.put(node, 2);
        return false;
    }

    private GraphIR buildIR(
        GraphDefinition graph,
        ResolvedGraph resolved
    ) {

        Comparator<NodeDefinition> byId =
            Comparator.comparing(
                n -> n.id().value()
            );

        List<NodeDefinition> sortedNodes =
            new ArrayList<>(graph.nodes());

        sortedNodes.sort(byId);

        List<IRNode> irNodes = new ArrayList<>();

        for (NodeDefinition node : sortedNodes) {
            ResolvedNode rn = findResolved(
                resolved, node.id()
            );

            irNodes.add(
                new IRNode(
                    node.id(),
                    node.type(),
                    node.config(),
                    rn != null
                        ? rn.resolvedReferences()
                        : Map.of()
                )
            );
        }

        List<EdgeDefinition> sortedEdges =
            new ArrayList<>(graph.edges());

        sortedEdges.sort(
            Comparator.comparing(
                e -> e.id().value()
            )
        );

        List<IREdge> irEdges = new ArrayList<>();

        for (EdgeDefinition edge : sortedEdges) {
            irEdges.add(
                new IREdge(
                    edge.id(),
                    edge.source(),
                    edge.target(),
                    edge.type(),
                    edge.condition()
                )
            );
        }

        Map<String, Object> allResolved =
            new TreeMap<>();

        for (ResolvedNode rn : resolved.nodes()) {
            if (!rn.resolvedReferences().isEmpty()) {
                allResolved.put(
                    rn.definition().id().value(),
                    rn.resolvedReferences()
                );
            }
        }

        return new GraphIR(
            graph.id(),
            graph.version(),
            COMPILER_VERSION,
            irNodes,
            irEdges,
            Map.copyOf(allResolved),
            Map.of()
        );
    }

    private ExecutionPlan buildPlan(
        GraphDefinition graph,
        ResolvedGraph resolved,
        GraphIR graphIR
    ) {

        // Build routing map
        // G3-1: Preserve edge type and condition
        // in routing entries
        Map<EntityId, List<RoutingEntry>> routing =
            new LinkedHashMap<>();

        List<EdgeDefinition> sortedEdges =
            new ArrayList<>(graph.edges());

        sortedEdges.sort(
            Comparator.comparing(
                e -> e.id().value()
            )
        );

        for (EdgeDefinition edge : sortedEdges) {
            routing
                .computeIfAbsent(
                    edge.source(),
                    k -> new ArrayList<>()
                )
                .add(new RoutingEntry(
                    edge.target(),
                    edge.type(),
                    edge.condition()
                ));
        }

        // Find entry node (START)
        EntityId entryNode = null;
        for (NodeDefinition node : graph.nodes()) {
            if (node.type() == NodeType.START) {
                entryNode = node.id();
                break;
            }
        }

        // Build execution nodes (sorted by ID for determinism)
        List<NodeDefinition> sortedNodes =
            new ArrayList<>(graph.nodes());

        sortedNodes.sort(
            Comparator.comparing(
                n -> n.id().value()
            )
        );

        List<ExecutionNode> execNodes = new ArrayList<>();

        for (NodeDefinition node : sortedNodes) {
            ResolvedNode rn = findResolved(
                resolved, node.id()
            );

            execNodes.add(
                new ExecutionNode(
                    node.id(),
                    node.type(),
                    node.config(),
                    rn != null
                        ? rn.resolvedReferences()
                        : Map.of()
                )
            );
        }

        // Compute fingerprint
        String fingerprint =
            computeFingerprint(graph, graphIR);

        return new ExecutionPlan(
            graph.id(),
            graph.version(),
            fingerprint,
            entryNode,
            execNodes,
            Map.copyOf(routing),
            graph.policies() != null
                ? graph.policies()
                : Map.of()
        );
    }

    private String computeFingerprint(
        GraphDefinition graph,
        GraphIR graphIR
    ) {

        StringBuilder sb = new StringBuilder();

        sb.append("GRAPH:");
        sb.append(graph.id().value());
        sb.append(":");
        sb.append(graph.version().value());
        sb.append("\n");

        sb.append("COMPILER:");
        sb.append(COMPILER_VERSION);
        sb.append("\n");

        // Deterministic node ordering
        List<IRNode> sortedNodes =
            new ArrayList<>(graphIR.nodes());

        sortedNodes.sort(
            Comparator.comparing(
                n -> n.id().value()
            )
        );

        for (IRNode node : sortedNodes) {
            sb.append("NODE:");
            sb.append(node.id().value());
            sb.append(":");
            sb.append(node.type().name());
            // G3-2: Include node config in
            // fingerprint
            if (node.config() != null
                && !node.config().isEmpty()) {
                sb.append(":config=");
                sb.append(node.config().toString());
            }
            sb.append("\n");
        }

        // Deterministic edge ordering
        List<IREdge> sortedEdges =
            new ArrayList<>(graphIR.edges());

        sortedEdges.sort(
            Comparator.comparing(
                e -> e.id().value()
            )
        );

        for (IREdge edge : sortedEdges) {
            sb.append("EDGE:");
            sb.append(edge.id().value());
            sb.append(":");
            sb.append(edge.source().value());
            sb.append("->");
            sb.append(edge.target().value());
            sb.append(":");
            sb.append(edge.type().name());
            // G3-2: Include edge condition in
            // fingerprint
            if (edge.condition() != null
                && !edge.condition().isBlank()) {
                sb.append(":cond=");
                sb.append(edge.condition());
            }
            sb.append("\n");
        }

        // Resolved references (TreeMap for determinism)
        Map<String, Object> sorted =
            new TreeMap<>(graphIR.resolvedReferences());

        for (Map.Entry<String, Object> entry
            : sorted.entrySet()) {

            sb.append("REF:");
            sb.append(entry.getKey());
            sb.append("=");
            sb.append(entry.getValue());
            sb.append("\n");
        }

        // G3-2: Include graph policies in
        // fingerprint
        Map<String, Object> policies =
            graph.policies() != null
                ? graph.policies()
                : Map.of();

        if (!policies.isEmpty()) {
            Map<String, Object> sortedPolicies =
                new TreeMap<>(policies);
            for (Map.Entry<String, Object> entry
                : sortedPolicies.entrySet()) {
                sb.append("POLICY:");
                sb.append(entry.getKey());
                sb.append("=");
                sb.append(entry.getValue());
                sb.append("\n");
            }
        }

        try {
            MessageDigest digest =
                MessageDigest.getInstance("SHA-256");

            byte[] hash = digest.digest(
                sb.toString()
                    .getBytes(StandardCharsets.UTF_8)
            );

            StringBuilder hex = new StringBuilder();
            for (byte b : hash) {
                hex.append(
                    String.format("%02x", b)
                );
            }
            return hex.toString();

        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(
                "SHA-256 not available", e
            );
        }
    }

    private Set<String> bfs(
        List<String> starts,
        Map<String, List<String>> adjacency
    ) {

        Set<String> visited = new HashSet<>();
        LinkedList<String> queue = new LinkedList<>();

        for (String start : starts) {
            visited.add(start);
            queue.add(start);
        }

        while (!queue.isEmpty()) {
            String current = queue.poll();

            for (String neighbor
                : adjacency.getOrDefault(
                    current, List.of())) {

                if (visited.add(neighbor)) {
                    queue.add(neighbor);
                }
            }
        }

        return visited;
    }

    private ResolvedNode findResolved(
        ResolvedGraph resolved,
        EntityId nodeId
    ) {

        for (ResolvedNode rn : resolved.nodes()) {
            if (rn.definition().id().equals(nodeId)) {
                return rn;
            }
        }

        return null;
    }

    private boolean hasErrors(
        List<CompilationDiagnostic> diagnostics
    ) {

        return diagnostics.stream()
            .anyMatch(
                d -> d.severity()
                    == CompilationDiagnostic.Severity.ERROR
            );
    }
}
