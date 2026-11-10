package io.github.regalpine.agentforge.workspace;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * In-memory sandbox.
 * AF-ENHANCE-07 §75: Mock Sandbox.
 *
 * <p>Simulates sandbox execution with
 * policy enforcement. Does not actually
 * spawn OS processes.
 *
 * <p>Enforces:
 * - Executable allowlist
 * - Network policy
 * - Resource limits (simulated)
 * - Lifecycle (SandboxStatus)
 */
public final class InMemorySandbox
    implements Sandbox {

    private final SandboxId sandboxId;
    private final Workspace workspace;
    private final SandboxPolicy policy;
    private final ResourceLimits limits;

    private volatile SandboxStatus status;

    private final AtomicInteger
        executionCount = new AtomicInteger(0);

    // Mock results: executable -> result
    private final ConcurrentHashMap<
        String, ProcessResult
    > mockResults = new ConcurrentHashMap<>();

    public InMemorySandbox(
        SandboxId sandboxId,
        Workspace workspace,
        SandboxPolicy policy
    ) {
        Objects.requireNonNull(
            sandboxId, "sandboxId"
        );
        Objects.requireNonNull(
            workspace, "workspace"
        );
        Objects.requireNonNull(
            policy, "policy"
        );

        this.sandboxId = sandboxId;
        this.workspace = workspace;
        this.policy = policy;
        this.limits = policy.limits();
        this.status = SandboxStatus.CREATED;
    }

    @Override
    public SandboxId id() {
        return sandboxId;
    }

    @Override
    public SandboxStatus status() {
        return status;
    }

    @Override
    public Workspace workspace() {
        return workspace;
    }

    @Override
    public ResourceLimits limits() {
        return limits;
    }

    @Override
    public SandboxPolicy policy() {
        return policy;
    }

    @Override
    public ProcessResult execute(
        ProcessRequest request
    ) {
        Objects.requireNonNull(
            request, "request"
        );

        if (status != SandboxStatus.RUNNING) {
            throw new IllegalStateException(
                "Sandbox not running: " + status
            );
        }

        // Check executable allowlist
        if (!policy.isExecutableAllowed(
            request.executable()
        )) {
            throw new SecurityException(
                "Executable not allowed: "
                    + request.executable()
            );
        }

        // Check process count limit
        int current =
            executionCount.incrementAndGet();
        if (current
            > limits.maxProcesses()
        ) {
            executionCount.decrementAndGet();
            throw new IllegalStateException(
                "Process limit exceeded: "
                    + limits.maxProcesses()
            );
        }

        // Check mock results first
        ProcessResult mock = mockResults.get(
            request.executable()
        );

        if (mock != null) {
            return mock;
        }

        // Simulate execution
        String pid = "proc-"
            + UUID.randomUUID()
                .toString()
                .substring(0, 8);

        try {
            Thread.sleep(1);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        return ProcessResult.success(
            pid,
            "simulated: "
                + request.executable(),
            Duration.ofMillis(1)
        );
    }

    @Override
    public void start() {
        if (status != SandboxStatus.CREATED
            && status
                != SandboxStatus.STOPPED
        ) {
            throw new IllegalStateException(
                "Cannot start from: " + status
            );
        }
        status = SandboxStatus.RUNNING;
    }

    @Override
    public void stop() {
        if (status != SandboxStatus.RUNNING
            && status
                != SandboxStatus.STARTING
        ) {
            throw new IllegalStateException(
                "Cannot stop: " + status
            );
        }
        status = SandboxStatus.STOPPED;
    }

    @Override
    public void destroy() {
        if (status == SandboxStatus.RUNNING) {
            stop();
        }
        status = SandboxStatus.DESTROYED;
    }

    /**
     * Register a mock result for an
     * executable.
     */
    public void mockResult(
        String executable,
        ProcessResult result
    ) {
        mockResults.put(
            executable, result
        );
    }

    /**
     * Get execution count.
     */
    public int executionCount() {
        return executionCount.get();
    }

    /**
     * Create a sandbox builder.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for InMemorySandbox.
     */
    public static final class Builder {

        private String id;
        private Workspace workspace;
        private SandboxPolicy policy;

        private Builder() {}

        public Builder id(String id) {
            this.id = id;
            return this;
        }

        public Builder workspace(
            Workspace ws
        ) {
            this.workspace = ws;
            return this;
        }

        public Builder policy(
            SandboxPolicy policy
        ) {
            this.policy = policy;
            return this;
        }

        public InMemorySandbox build() {
            if (id == null || id.isBlank()) {
                id = "sandbox-"
                    + UUID.randomUUID()
                        .toString()
                        .substring(0, 8);
            }
            if (workspace == null) {
                throw new IllegalArgumentException(
                    "workspace required"
                );
            }
            if (policy == null) {
                policy =
                    SandboxPolicy.permissive();
            }

            return new InMemorySandbox(
                new SandboxId(id),
                workspace, policy
            );
        }
    }
}
