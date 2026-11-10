package io.github.regalpine.agentforge.distributed.redis;

import io.github.regalpine.agentforge.distributed.heartbeat.WorkerHeartbeat;
import io.github.regalpine.agentforge.distributed.heartbeat.WorkerRegistrationStore;
import io.github.regalpine.agentforge.distributed.worker.WorkerId;
import io.github.regalpine.agentforge.distributed.worker.WorkerRegistration;
import io.github.regalpine.agentforge.distributed.worker.WorkerStatus;
import io.lettuce.core.RedisClient;
import io.lettuce.core.RedisURI;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.api.sync.RedisCommands;

import java.time.Duration;
import java.time.Instant;
import java.util.*;

/**
 * Redis-backed {@link WorkerRegistrationStore} using a Redis Hash.
 * <p>
 * Key layout:
 * <ul>
 *   <li>{@code agentforge:workers} → Hash: workerId → registration fields</li>
 * </ul>
 * <p>
 * Each hash field stores a serialized registration:
 * {@code status|registeredAt|lastHeartbeat|cap1,cap2,...}
 */
public final class RedisWorkerRegistrationStore implements WorkerRegistrationStore {

    private static final String WORKERS_KEY = "agentforge:workers";
    private static final Duration DEFAULT_HEARTBEAT_TIMEOUT = Duration.ofSeconds(30);

    private final RedisCommands<String, String> commands;
    private final Duration heartbeatTimeout;

    public RedisWorkerRegistrationStore(String redisUri) {
        this(redisUri, DEFAULT_HEARTBEAT_TIMEOUT);
    }

    public RedisWorkerRegistrationStore(String redisUri, Duration heartbeatTimeout) {
        RedisClient client = RedisClient.create(RedisURI.create(redisUri));
        StatefulRedisConnection<String, String> connection = client.connect();
        this.commands = connection.sync();
        this.heartbeatTimeout = heartbeatTimeout;
    }

    @Override
    public void register(WorkerRegistration registration) {
        String field = registration.workerId().value();
        String value = serialize(registration);
        commands.hset(WORKERS_KEY, field, value);
    }

    @Override
    public void heartbeat(WorkerHeartbeat heartbeat) {
        String field = heartbeat.workerId().value();
        String existing = commands.hget(WORKERS_KEY, field);
        if (existing == null) return;

        WorkerRegistration reg = deserialize(field, existing);
        if (reg == null) return;

        WorkerRegistration updated = new WorkerRegistration(
            reg.workerId(),
            reg.status(),
            reg.capabilities(),
            reg.registeredAt(),
            heartbeat.timestamp()
        );
        commands.hset(WORKERS_KEY, field, serialize(updated));
    }

    @Override
    public List<WorkerRegistration> activeWorkers() {
        Map<String, String> all = commands.hgetall(WORKERS_KEY);
        if (all == null || all.isEmpty()) return List.of();

        List<WorkerRegistration> result = new ArrayList<>();
        for (var entry : all.entrySet()) {
            WorkerRegistration reg = deserialize(entry.getKey(), entry.getValue());
            if (reg != null && reg.status() == WorkerStatus.ACTIVE) {
                result.add(reg);
            }
        }
        return List.copyOf(result);
    }

    @Override
    public boolean isAlive(WorkerId workerId) {
        String value = commands.hget(WORKERS_KEY, workerId.value());
        if (value == null) return false;

        WorkerRegistration reg = deserialize(workerId.value(), value);
        if (reg == null) return false;
        if (reg.status() != WorkerStatus.ACTIVE) return false;

        return reg.lastHeartbeat()
            .plus(heartbeatTimeout)
            .isAfter(Instant.now());
    }

    // ── Serialization ──

    private static String serialize(WorkerRegistration reg) {
        String caps = String.join(",", reg.capabilities());
        return reg.status().name()
            + "|" + reg.registeredAt().toEpochMilli()
            + "|" + reg.lastHeartbeat().toEpochMilli()
            + "|" + caps;
    }

    private static WorkerRegistration deserialize(String workerIdStr, String value) {
        try {
            String[] parts = value.split("\\|", -1);
            if (parts.length < 4) return null;

            WorkerStatus status = WorkerStatus.valueOf(parts[0]);
            Instant registeredAt = Instant.ofEpochMilli(Long.parseLong(parts[1]));
            Instant lastHeartbeat = Instant.ofEpochMilli(Long.parseLong(parts[2]));
            Set<String> capabilities;
            if (parts[3].isEmpty()) {
                capabilities = Set.of();
            } else {
                capabilities = Set.of(parts[3].split(","));
            }

            return new WorkerRegistration(
                new WorkerId(workerIdStr),
                status, capabilities,
                registeredAt, lastHeartbeat
            );
        } catch (Exception e) {
            return null;
        }
    }
}
