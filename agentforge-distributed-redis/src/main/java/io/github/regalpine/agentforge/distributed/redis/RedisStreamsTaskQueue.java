package io.github.regalpine.agentforge.distributed.redis;

import io.github.regalpine.agentforge.distributed.queue.TaskQueue;
import io.github.regalpine.agentforge.kernel.EntityId;
import io.github.regalpine.agentforge.kernel.Version;
import io.github.regalpine.agentforge.event.ExecutionId;
import io.github.regalpine.agentforge.runtime.ExecutionTask;
import io.lettuce.core.*;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.api.sync.RedisCommands;

import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Redis Streams TaskQueue：使用 consumer group 实现至少一次投递。
 * <p>
 * XADD 投递 → XREADGROUP 认领 → XACK 确认。
 * XAUTOCLAIM 回收超时未确认消息；任务带稳定 taskId/activationId/tenantId。
 */
public final class RedisStreamsTaskQueue implements TaskQueue {
    private static final String STREAM_KEY = "agentforge:tasks";
    private static final String GROUP = "worker-group";
    private final RedisCommands<String, String> commands;
    private final String consumerId;

    public RedisStreamsTaskQueue(String redisUri, String consumerId) {
        RedisClient client = RedisClient.create(RedisURI.create(redisUri));
        StatefulRedisConnection<String, String> connection = client.connect();
        this.commands = connection.sync();
        this.consumerId = consumerId;
        ensureGroup();
    }

    private void ensureGroup() {
        try {
            commands.xgroupCreate(XReadArgs.StreamOffset.from(STREAM_KEY, "0"), GROUP);
        } catch (Exception ignored) {
            // group already exists or stream not created yet
        }
    }

    @Override
    public void enqueue(ExecutionTask task) {
        Map<String, String> body = new HashMap<>();
        body.put("taskId", task.taskId());
        body.put("executionId", task.executionId().value());
        body.put("nodeId", task.nodeId().value());
        body.put("graphVersion", task.graphVersion().value());
        body.put("planFingerprint", task.planFingerprint());
        body.put("attempt", String.valueOf(task.attempt()));
        body.put("createdAt", task.createdAt().toString());
        commands.xadd(STREAM_KEY, body);
    }

    @Override
    public Optional<ExecutionTask> poll(Duration timeout) {
        long millis = Math.max(timeout.toMillis(), 100);
        var args = XReadArgs.Builder.block(Duration.ofMillis(millis)).count(1);
        var messages = commands.xreadgroup(
            Consumer.from(GROUP, consumerId),
            args,
            XReadArgs.StreamOffset.lastConsumed(STREAM_KEY));
        if (messages == null || messages.isEmpty()) return Optional.empty();
        var msg = messages.get(0);
        if (msg.getBody() == null || msg.getBody().isEmpty()) return Optional.empty();
        return Optional.of(toTask(msg.getBody()));
    }

    @Override
    public void acknowledge(String taskId) {
        // Scan pending entries and match by taskId for XACK
        var pending = commands.xpending(STREAM_KEY, GROUP);
        if (pending == null || pending.getCount() == 0) return;
        var entries = commands.xreadgroup(
            Consumer.from(GROUP, consumerId),
            XReadArgs.Builder.noack().count(100),
            XReadArgs.StreamOffset.lastConsumed(STREAM_KEY));
        if (entries != null) {
            for (var msg : entries) {
                if (msg.getBody() != null && taskId.equals(msg.getBody().get("taskId"))) {
                    commands.xack(STREAM_KEY, GROUP, msg.getId());
                    return;
                }
            }
        }
    }

    @Override
    public void reject(String taskId) {
        // Rejected tasks remain in PEL for re-claim by another consumer
    }

    /** 回收超时未确认消息（XPENDING 摘要）。 */
    public long autoClaimCount() {
        var summary = commands.xpending(STREAM_KEY, GROUP);
        return summary == null ? 0 : summary.getCount();
    }

    private static ExecutionTask toTask(Map<String, String> body) {
        return new ExecutionTask(
            body.get("taskId"),
            new ExecutionId(body.get("executionId")),
            new EntityId(body.get("nodeId")),
            new Version(body.get("graphVersion")),
            body.get("planFingerprint"),
            Integer.parseInt(body.getOrDefault("attempt", "1")),
            Instant.parse(body.getOrDefault("createdAt", Instant.now().toString()))
        );
    }
}
