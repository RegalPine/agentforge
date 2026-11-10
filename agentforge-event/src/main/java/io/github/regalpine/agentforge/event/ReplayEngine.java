package io.github.regalpine.agentforge.event;

import java.util.List;

/**
 * 重放引擎。
 *
 * <p>支持 State Replay 和 Execution Replay。</p>
 */
public final class ReplayEngine {

    private final EnvelopeEventStore store;
    private final CheckpointEngine checkpointEngine;

    public ReplayEngine(
        EnvelopeEventStore store,
        CheckpointEngine checkpointEngine
    ) {
        this.store = store;
        this.checkpointEngine = checkpointEngine;
    }

    /**
     * State Replay：从事件流恢复状态。
     */
    public ReplayResult stateReplay(
        String executionId
    ) {
        Checkpoint cp =
            checkpointEngine.latest(executionId);
        long fromSeq = 0;
        if (cp != null) {
            fromSeq = cp.sequence();
        }
        List<EventEnvelope> events;
        if (fromSeq > 0) {
            events = store.readAfter(
                executionId, fromSeq
            );
        } else {
            events = store.read(executionId);
        }
        return new ReplayResult(
            executionId,
            cp,
            events,
            ReplayMode.REPLAY
        );
    }

    /**
     * 获取完整 Trace。
     */
    public ExecutionTrace trace(
        String executionId
    ) {
        List<EventEnvelope> events =
            store.read(executionId);
        return ExecutionTrace.fromEvents(
            executionId, events
        );
    }

    /**
     * 重放结果。
     */
    public record ReplayResult(
        String executionId,
        Checkpoint fromCheckpoint,
        List<EventEnvelope> events,
        ReplayMode mode
    ) {

        public boolean hasCheckpoint() {
            return fromCheckpoint != null;
        }

        public int eventCount() {
            return events.size();
        }
    }
}
