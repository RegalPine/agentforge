package io.github.regalpine.agentforge.server.service;

import io.github.regalpine.agentforge.compiler.CompilationContext;
import io.github.regalpine.agentforge.compiler.CompilationResult;
import io.github.regalpine.agentforge.compiler.DefaultGraphCompiler;
import io.github.regalpine.agentforge.compiler.GraphCompiler;
import io.github.regalpine.agentforge.compiler.RegistrySnapshot;
import io.github.regalpine.agentforge.graph.GraphDefinition;
import io.github.regalpine.agentforge.graph.ir.ExecutionPlan;
import io.github.regalpine.agentforge.graph.ir.ExecutionPlanRegistry;
import io.github.regalpine.agentforge.kernel.EntityId;
import io.github.regalpine.agentforge.kernel.Version;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public final class GraphService {

    private record GraphKey(String tenantId, String graphId, String version) {}

    private final Map<GraphKey, GraphDefinition> graphs =
        new ConcurrentHashMap<>();

    private final Map<GraphKey, ExecutionPlan> plans =
        new ConcurrentHashMap<>();

    private final GraphCompiler compiler;
    private final ExecutionPlanRegistry persistentRegistry;

    public GraphService() {
        this.compiler = new DefaultGraphCompiler();
        this.persistentRegistry = null;
    }

    public GraphService(
        GraphCompiler compiler
    ) {
        this.compiler = compiler;
        this.persistentRegistry = null;
    }

    public GraphService(
        GraphCompiler compiler,
        ExecutionPlanRegistry persistentRegistry
    ) {
        this.compiler = compiler;
        this.persistentRegistry = persistentRegistry;
    }

    public void publish(GraphDefinition graph, RegistrySnapshot snapshot) {
        publish("default", graph, snapshot);
    }

    public void publish(io.github.regalpine.agentforge.security.SecurityContext context,
                        GraphDefinition graph, RegistrySnapshot snapshot) {
        ServiceAuthorization.require(context, "graph:publish");
        publish(context.tenant().tenantId(), graph, snapshot);
    }

    public synchronized void publish(String tenantId, GraphDefinition graph, RegistrySnapshot snapshot) {
        requireTenant(tenantId);
        CompilationContext context =
            new CompilationContext(
                snapshot,
                null
            );

        CompilationResult result =
            compiler.compile(graph, context);

        if (result instanceof CompilationResult.Failure fail) {
            throw new IllegalStateException(
                "Compilation failed: " +
                fail.diagnostics()
            );
        }

        ExecutionPlan plan =
            ((CompilationResult.Success) result)
                .executionPlan();

        GraphKey key = new GraphKey(tenantId, graph.id().value(), graph.version().value());

        graphs.put(key, graph);
        plans.put(key, plan);

        // Persist to durable registry
        if (persistentRegistry != null) {
            persistentRegistry.register(plan);
        }
    }

    public Optional<GraphDefinition> findGraph(
        String graphId,
        String version
    ) {
        return findGraph("default", graphId, version);
    }

    public Optional<ExecutionPlan> findPlan(
        String graphId,
        String version
    ) {
        return findPlan("default", graphId, version);
    }

    public synchronized Optional<GraphDefinition> findGraph(String tenantId, String graphId, String version) {
        requireTenant(tenantId);
        return Optional.ofNullable(graphs.get(new GraphKey(tenantId, graphId, version)));
    }

    /**
     * Store a graph definition without compiling it.
     * Used by POST /api/v1/graphs to create a graph that can later be compiled.
     */
    public synchronized void storeGraph(String tenantId, GraphDefinition graph) {
        requireTenant(tenantId);
        GraphKey key = new GraphKey(tenantId, graph.id().value(), graph.version().value());
        if (graphs.containsKey(key)) {
            throw new IllegalStateException("Graph already exists: " + graph.id().value() + "@" + graph.version().value());
        }
        graphs.put(key, graph);
    }

    /**
     * List all graph summaries for a tenant.
     */
    public synchronized List<Map<String, Object>> listGraphs(String tenantId) {
        requireTenant(tenantId);
        return graphs.entrySet().stream()
            .filter(e -> e.getKey().tenantId().equals(tenantId))
            .map(e -> {
                GraphDefinition g = e.getValue();
                return Map.<String, Object>of(
                    "id", g.id().value(),
                    "version", g.version().value(),
                    "name", g.name(),
                    "description", g.description() != null ? g.description() : "",
                    "nodeCount", g.nodes().size(),
                    "edgeCount", g.edges().size(),
                    "status", plans.containsKey(e.getKey()) ? "PUBLISHED" : "DRAFT",
                    "createdAt", g.metadata().getOrDefault("createdAt", "").toString()
                );
            })
            .collect(Collectors.toList());
    }

    public Optional<GraphDefinition> findGraph(io.github.regalpine.agentforge.security.SecurityContext context,
                                                String graphId, String version) {
        ServiceAuthorization.require(context, "graph:read");
        return findGraph(context.tenant().tenantId(), graphId, version);
    }

    public synchronized Optional<ExecutionPlan> findPlan(String tenantId, String graphId, String version) {
        requireTenant(tenantId);
        ExecutionPlan cached = plans.get(new GraphKey(tenantId, graphId, version));
        if (cached != null) {
            return Optional.of(cached);
        }
        // Fall back to persistent registry (DB)
        if (persistentRegistry != null) {
            Optional<ExecutionPlan> fromDb = persistentRegistry.find(
                new EntityId(graphId), new Version(version));
            fromDb.ifPresent(plan -> plans.put(new GraphKey(tenantId, graphId, version), plan));
            return fromDb;
        }
        return Optional.empty();
    }

    private static void requireTenant(String tenantId) {
        if (tenantId == null || tenantId.isBlank()) throw new SecurityException("Tenant is required");
    }
}
