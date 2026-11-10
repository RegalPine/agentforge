package io.github.regalpine.agentforge.memory.redis;

/**
 * Redis serialisation for memory records.
 * AF-ENHANCE-03 §5: converts MemoryRecord
 * to/from a Redis-friendly string format.
 *
 * <p>Uses a simple pipe-delimited format
 * to avoid external JSON dependencies.
 * Production deployments should use a
 * proper JSON or MessagePack codec.
 */
final class RedisMemoryCodec {

    private static final String SEP = "\u001F";

    private RedisMemoryCodec() {}

    /**
     * Encode a memory record into a
     * Redis hash-friendly format.
     */
    static String encode(
        String memoryId,
        String tenantId,
        String subjectId,
        String type,
        String scope,
        String content,
        long createdAtMs,
        long updatedAtMs,
        long expiresAtMs,
        String status,
        String source,
        double confidenceScore,
        String confidenceBasis,
        String importance,
        String classification,
        long version
    ) {
        return String.join(SEP,
            memoryId,
            tenantId,
            subjectId,
            type,
            scope,
            content,
            String.valueOf(createdAtMs),
            String.valueOf(updatedAtMs),
            String.valueOf(expiresAtMs),
            status,
            source,
            String.valueOf(confidenceScore),
            confidenceBasis,
            importance,
            classification,
            String.valueOf(version)
        );
    }

    /**
     * Decode a Redis value back into
     * its component fields.
     */
    static String[] decode(String encoded) {
        return encoded.split(SEP, -1);
    }

    static final int F_MEMORY_ID = 0;
    static final int F_TENANT_ID = 1;
    static final int F_SUBJECT_ID = 2;
    static final int F_TYPE = 3;
    static final int F_SCOPE = 4;
    static final int F_CONTENT = 5;
    static final int F_CREATED_AT = 6;
    static final int F_UPDATED_AT = 7;
    static final int F_EXPIRES_AT = 8;
    static final int F_STATUS = 9;
    static final int F_SOURCE = 10;
    static final int F_CONF_SCORE = 11;
    static final int F_CONF_BASIS = 12;
    static final int F_IMPORTANCE = 13;
    static final int F_CLASSIFICATION = 14;
    static final int F_VERSION = 15;
}
