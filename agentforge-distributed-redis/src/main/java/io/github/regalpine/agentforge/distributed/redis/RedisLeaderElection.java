package io.github.regalpine.agentforge.distributed.redis;

import io.github.regalpine.agentforge.distributed.coordination.LeaderElection;
import io.lettuce.core.RedisClient;
import io.lettuce.core.RedisURI;
import io.lettuce.core.SetArgs;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.api.sync.RedisCommands;

/**
 * Redis-backed {@link LeaderElection} using SET NX + PX for leader key.
 * <p>
 * The leader holds the key {@code agentforge:leader} with its instance ID
 * as the value. The key has a TTL so that if the leader crashes,
 * another instance can acquire leadership after the TTL expires.
 * <p>
 * The local instance periodically renews the TTL via {@link #tryAcquireLeadership()}
 * (called on a schedule by the reconciler).
 */
public final class RedisLeaderElection implements LeaderElection {

    private static final String LEADER_KEY = "agentforge:leader";
    private static final long DEFAULT_TTL_MILLIS = 30_000;

    private final RedisCommands<String, String> commands;
    private final String instanceId;
    private final long ttlMillis;
    private volatile boolean isLeader = false;

    public RedisLeaderElection(String redisUri, String instanceId) {
        this(redisUri, instanceId, DEFAULT_TTL_MILLIS);
    }

    public RedisLeaderElection(String redisUri, String instanceId, long ttlMillis) {
        RedisClient client = RedisClient.create(RedisURI.create(redisUri));
        StatefulRedisConnection<String, String> connection = client.connect();
        this.commands = connection.sync();
        this.instanceId = instanceId;
        this.ttlMillis = ttlMillis;
    }

    @Override
    public boolean tryAcquireLeadership() {
        // Try to SET the leader key only if it doesn't exist
        String result = commands.set(
            LEADER_KEY, instanceId,
            SetArgs.Builder.nx().px(ttlMillis)
        );

        if ("OK".equals(result)) {
            isLeader = true;
            return true;
        }

        // Key exists — check if we already own it (renewal)
        String current = commands.get(LEADER_KEY);
        if (instanceId.equals(current)) {
            // Renew our leadership
            commands.pexpire(LEADER_KEY, ttlMillis);
            isLeader = true;
            return true;
        }

        isLeader = false;
        return false;
    }

    @Override
    public void releaseLeadership() {
        // Only delete if we still own it (Lua for atomicity)
        String script =
            "if redis.call('GET', KEYS[1]) == ARGV[1] then " +
            "  return redis.call('DEL', KEYS[1]) " +
            "else " +
            "  return 0 " +
            "end";

        commands.eval(script, io.lettuce.core.ScriptOutputType.INTEGER,
            new String[]{LEADER_KEY}, instanceId);
        isLeader = false;
    }

    @Override
    public boolean isLeader() {
        if (!isLeader) return false;
        // Double-check: verify we still own the key
        String current = commands.get(LEADER_KEY);
        if (!instanceId.equals(current)) {
            isLeader = false;
            return false;
        }
        return true;
    }
}
