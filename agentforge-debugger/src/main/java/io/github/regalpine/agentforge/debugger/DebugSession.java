package io.github.regalpine.agentforge.debugger;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * 调试会话。
 *
 * <p>管理断点、步进控制、状态观察。
 * 对应 AF-ENHANCE-04 §7。</p>
 */
public final class DebugSession {

    private final String id;
    private final String executionId;
    private DebugSessionStatus status;
    private ExecutionCursor cursor;
    private final Map<String, Breakpoint>
        breakpoints = new ConcurrentHashMap<>();
    private final Instant createdAt;
    private final List<DebugEventListener>
        listeners = new CopyOnWriteArrayList<>();
    private final List<DebugOverride>
        overrides = new ArrayList<>();
    private ExecutionSnapshot lastSnapshot;

    public DebugSession(
        String executionId
    ) {
        Objects.requireNonNull(
            executionId, "executionId"
        );
        this.id = "debug-"
            + UUID.randomUUID();
        this.executionId = executionId;
        this.status = DebugSessionStatus
            .CREATED;
        this.createdAt = Instant.now();
    }

    public String id() { return id; }

    public String executionId() {
        return executionId;
    }

    public DebugSessionStatus status() {
        return status;
    }

    public ExecutionCursor cursor() {
        return cursor;
    }

    public Instant createdAt() {
        return createdAt;
    }

    public List<DebugOverride> overrides() {
        return List.copyOf(overrides);
    }

    // --- Lifecycle ---

    /**
     * 附加到执行。
     */
    public void attach() {
        transitionTo(
            DebugSessionStatus.ATTACHED
        );
    }

    /**
     * 开始运行。
     */
    public void resume() {
        transitionTo(
            DebugSessionStatus.RUNNING
        );
    }

    /**
     * 暂停。
     */
    public void pause() {
        transitionTo(
            DebugSessionStatus.PAUSED
        );
    }

    /**
     * 开始重放。
     */
    public void replay() {
        transitionTo(
            DebugSessionStatus.REPLAYING
        );
    }

    /**
     * 完成。
     */
    public void complete() {
        transitionTo(
            DebugSessionStatus.COMPLETED
        );
    }

    /**
     * 终止。
     */
    public void terminate() {
        transitionTo(
            DebugSessionStatus.TERMINATED
        );
    }

    // --- Breakpoints ---

    /**
     * 添加断点。
     */
    public void addBreakpoint(
        Breakpoint breakpoint
    ) {
        Objects.requireNonNull(
            breakpoint, "breakpoint"
        );
        breakpoints.put(
            breakpoint.id(),
            breakpoint
        );
    }

    /**
     * 移除断点。
     */
    public void removeBreakpoint(
        String breakpointId
    ) {
        breakpoints.remove(breakpointId);
    }

    /**
     * 获取所有断点。
     */
    public List<Breakpoint>
    getBreakpoints() {
        return List.copyOf(
            breakpoints.values()
        );
    }

    /**
     * 检查是否触发断点。
     */
    public Breakpoint checkBreakpoints(
        DebugContext context
    ) {
        for (Breakpoint bp
            : breakpoints.values()
        ) {
            if (bp.shouldTrigger(context)) {
                pause();
                notifyBreakpointHit(
                    bp, context
                );
                return bp;
            }
        }
        return null;
    }

    // --- Step Control ---

    /**
     * 执行步进命令。
     */
    public void step(
        StepCommand command
    ) {
        Objects.requireNonNull(
            command, "command"
        );
        switch (command) {
            case CONTINUE -> resume();
            case PAUSE -> pause();
            case STEP_OVER,
                 STEP_INTO,
                 STEP_OUT -> {
                if (status
                    != DebugSessionStatus
                        .PAUSED
                ) {
                    throw new IllegalStateException(
                        "Must be paused to step"
                    );
                }
            }
            case RESTART -> {
                transitionTo(
                    DebugSessionStatus
                        .RUNNING
                );
            }
            case STOP -> terminate();
        }
    }

    // --- State Inspection ---

    /**
     * 更新游标。
     */
    public void updateCursor(
        ExecutionCursor cursor
    ) {
        this.cursor = cursor;
    }

    /**
     * 更新快照。
     */
    public void updateSnapshot(
        ExecutionSnapshot snapshot
    ) {
        this.lastSnapshot = snapshot;
    }

    /**
     * 获取当前快照。
     */
    public ExecutionSnapshot snapshot() {
        return lastSnapshot;
    }

    // --- Override ---

    /**
     * 添加调试覆盖。
     */
    public void addOverride(
        DebugOverride override
    ) {
        overrides.add(override);
    }

    // --- Listeners ---

    public void addListener(
        DebugEventListener listener
    ) {
        listeners.add(listener);
    }

    // --- Internal ---

    private void transitionTo(
        DebugSessionStatus newStatus
    ) {
        this.status = newStatus;
    }

    private void notifyBreakpointHit(
        Breakpoint bp,
        DebugContext context
    ) {
        for (var listener
            : listeners
        ) {
            listener.onBreakpointHit(
                this, bp, context
            );
        }
    }

    /**
     * 调试事件监听器。
     */
    public interface
    DebugEventListener {
        void onBreakpointHit(
            DebugSession session,
            Breakpoint breakpoint,
            DebugContext context
        );
    }
}
