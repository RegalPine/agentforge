package io.github.regalpine.agentforge.fault;

/**
 * Common fault points used across AgentForge modules.
 * These are the instrumentation sites where faults can be injected.
 */
public final class FaultPoints {

    private FaultPoints() {
    }

    public static final FaultPoint COMPILER_COMPILE =
            new FaultPoint("compiler.compile", "Graph compilation");

    public static final FaultPoint COMPILER_TOPOLOGICAL_SORT =
            new FaultPoint("compiler.topologicalSort", "Topological sort during compilation");

    public static final FaultPoint RUNTIME_START =
            new FaultPoint("runtime.start", "Execution start");

    public static final FaultPoint RUNTIME_NODE_EXECUTE =
            new FaultPoint("runtime.node.execute", "Node execution");

    public static final FaultPoint RUNTIME_SCHEDULER_ACQUIRE_LEASE =
            new FaultPoint("runtime.scheduler.acquireLease", "Scheduler lease acquisition");

    public static final FaultPoint RUNTIME_SCHEDULER_HEARTBEAT =
            new FaultPoint("runtime.scheduler.heartbeat", "Scheduler heartbeat");

    public static final FaultPoint PERSISTENCE_SAVE_STATE =
            new FaultPoint("persistence.saveState", "Save execution state");

    public static final FaultPoint PERSISTENCE_LOAD_STATE =
            new FaultPoint("persistence.loadState", "Load execution state");

    public static final FaultPoint PERSISTENCE_APPEND_EVENT =
            new FaultPoint("persistence.appendEvent", "Append event");

    public static final FaultPoint PERSISTENCE_CHECKPOINT =
            new FaultPoint("persistence.checkpoint", "Create checkpoint");

    public static final FaultPoint PROVIDER_CALL =
            new FaultPoint("provider.call", "Provider capability execution");

    public static final FaultPoint HTTP_PROVIDER_REQUEST =
            new FaultPoint("http.provider.request", "HTTP provider outbound request");

    public static final FaultPoint SECURITY_TOKEN_VALIDATE =
            new FaultPoint("security.token.validate", "Token validation");

    public static final FaultPoint SECURITY_AUTHORIZATION_CHECK =
            new FaultPoint("security.authorization.check", "Authorization check");
}
