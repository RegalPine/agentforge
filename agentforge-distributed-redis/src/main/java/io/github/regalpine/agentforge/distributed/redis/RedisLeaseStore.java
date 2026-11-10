package io.github.regalpine.agentforge.distributed.redis;

import io.github.regalpine.agentforge.distributed.lease.ExecutionLease;
import io.github.regalpine.agentforge.distributed.lease.LeaseStore;
import io.github.regalpine.agentforge.distributed.worker.WorkerId;
import io.github.regalpine.agentforge.event.ExecutionId;
import io.lettuce.core.RedisClient;
import io.lettuce.core.RedisURI;
import io.lettuce.core.SetArgs;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.api.sync.RedisCommands;

import java.time.Instant;
import java.util.Optional;

/**
 * Redis-backed {@link LeaseStore} using SET NX + TTL for distributed leases.
 * <p>
 * Redis key layout:
 * <ul>
 *   <li>{@code agentforge:lease:{executionId}} → {@code workerId:fencingToken:expiresAtEpochMillis}</li>
 *   <li>{@code agentforge:lease:token-counter} → monotonically increasing fencing token</li>
 * </ul>
 * <p>
 * Renew and release use Lua scripts for atomic check-and-update.
 */
public final class RedisLeaseStore implements LeaseStore {

    private static final String LEASE_PREFIX = "agentforge:lease:";
    private static final String TOKEN_COUNTER_KEY = LEASE_PREFIX + "token-counter";

    /** Lua: renew lease only if worker + fencing token match and not expired. */
    private static final String RENEW_SCRIPT =
        "local val = redis.call('GET', KEYS[1]) " +
        "if val == false then return 0 end " +
        "local parts = {} " +
        "for p in string.gmatch(val, '[^:]+') do parts[#parts+1] = p end " +
        "if parts[1] ~= ARGV[1] then return 0 end " +
        "if tonumber(parts[2]) ~= tonumber(ARGV[2]) then return 0 end " +
        "if tonumber(parts[3]) < tonumber(ARGV[3]) then return 0 end " +
        "redis.call('SET', KEYS[1], parts[1] .. ':' .. parts[2] .. ':' .. ARGV[4], 'KEEPTTL') " +
        "return 1";

    /** Lua: release lease only if worker + fencing token match. */
    private static final String RELEASE_SCRIPT =
        "local val = redis.call('GET', KEYS[1]) " +
        "if val == false then return 1 end " +
        "local parts = {} " +
        "for p in string.gmatch(val, '[^:]+') do parts[#parts+1] = p end " +
        "if parts[1] ~= ARGV[1] then return 0 end " +
        "if tonumber(parts[2]) ~= tonumber(ARGV[2]) then return 0 end " +
        "redis.call('DEL', KEYS[1]) " +
        "return 1";

    private final RedisCommands<String, String> commands;

    public RedisLeaseStore(String redisUri) {
        RedisClient client = RedisClient.create(RedisURI.create(redisUri));
        StatefulRedisConnection<String, String> connection = client.connect();
        this.commands = connection.sync();
    }

    @Override
    public Optional<ExecutionLease> acquire(ExecutionId executionId, WorkerId workerId, long durationMillis) {
        String key = LEASE_PREFIX + executionId.value();
        long nowMillis = System.currentTimeMillis();

        // Atomic check-and-set: only acquire if key doesn't exist or is expired
        // Use Lua for atomic check + set
        // KEYS[1] = lease key
        // ARGV[1] = workerId, ARGV[2] = nowMillis, ARGV[3] = durationMillis, ARGV[4] = token counter key
        String acquireScript =
            "local val = redis.call('GET', KEYS[1]) " +
            "if val ~= false then " +
            "  local parts = {} " +
            "  for p in string.gmatch(val, '[^:]+') do parts[#parts+1] = p end " +
            "  if tonumber(parts[3]) > tonumber(ARGV[2]) then return '' end " +
            "end " +
            "local token = redis.call('INCR', ARGV[4]) " +
            "local expires = tonumber(ARGV[2]) + tonumber(ARGV[3]) " +
            "redis.call('SET', KEYS[1], ARGV[1] .. ':' .. token .. ':' .. expires, 'PX', ARGV[3]) " +
            "return ARGV[1] .. ':' .. token .. ':' .. expires";

        String result = (String) commands.eval(
            acquireScript,
            io.lettuce.core.ScriptOutputType.VALUE,
            new String[]{key},
            workerId.value(),
            String.valueOf(nowMillis),
            String.valueOf(durationMillis),
            TOKEN_COUNTER_KEY
        );

        if (result == null || result.isEmpty()) {
            return Optional.empty();
        }

        return Optional.of(parseLease(executionId, result));
    }

    @Override
    public boolean renew(ExecutionId executionId, WorkerId workerId, long fencingToken, long durationMillis) {
        String key = LEASE_PREFIX + executionId.value();
        long nowMillis = System.currentTimeMillis();
        long newExpires = nowMillis + durationMillis;

        Long result = (Long) commands.eval(
            RENEW_SCRIPT,
            io.lettuce.core.ScriptOutputType.INTEGER,
            new String[]{key},
            workerId.value(),
            String.valueOf(fencingToken),
            String.valueOf(nowMillis),
            String.valueOf(newExpires)
        );

        return result != null && result == 1L;
    }

    @Override
    public boolean release(ExecutionId executionId, WorkerId workerId, long fencingToken) {
        String key = LEASE_PREFIX + executionId.value();

        Long result = (Long) commands.eval(
            RELEASE_SCRIPT,
            io.lettuce.core.ScriptOutputType.INTEGER,
            new String[]{key},
            workerId.value(),
            String.valueOf(fencingToken)
        );

        return result != null && result == 1L;
    }

    @Override
    public Optional<ExecutionLease> current(ExecutionId executionId) {
        String key = LEASE_PREFIX + executionId.value();
        String value = commands.get(key);
        if (value == null) {
            return Optional.empty();
        }
        return Optional.of(parseLease(executionId, value));
    }

    private static ExecutionLease parseLease(ExecutionId executionId, String value) {
        // Format: workerId:fencingToken:expiresAtMillis
        int firstColon = value.indexOf(':');
        int secondColon = value.indexOf(':', firstColon + 1);
        String workerIdStr = value.substring(0, firstColon);
        long token = Long.parseLong(value.substring(firstColon + 1, secondColon));
        long expiresMillis = Long.parseLong(value.substring(secondColon + 1));

        return new ExecutionLease(
            executionId,
            new WorkerId(workerIdStr),
            token,
            Instant.ofEpochMilli(expiresMillis - (expiresMillis - System.currentTimeMillis())),
            Instant.ofEpochMilli(expiresMillis)
        );
    }
}
