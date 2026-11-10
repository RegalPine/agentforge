package io.github.regalpine.agentforge.security.iam;

import java.time.Instant;
import java.util.Objects;
import java.util.Optional;

/** 会话权威存储；仅接受 token 摘要，禁止持久化 Bearer 明文。 */
public interface SessionStore {
    record SessionRecord(String tokenHash, String userId, String tenantId,
                         Instant createdAt, Instant expiresAt, Instant revokedAt) {
        public SessionRecord {
            Objects.requireNonNull(tokenHash);
            Objects.requireNonNull(userId);
            Objects.requireNonNull(tenantId);
            Objects.requireNonNull(createdAt);
            Objects.requireNonNull(expiresAt);
            if (!expiresAt.isAfter(createdAt)) throw new IllegalArgumentException("Invalid session lifetime");
        }
        public boolean activeAt(Instant now) { return revokedAt == null && expiresAt.isAfter(now); }
    }

    void create(SessionRecord session);
    Optional<SessionRecord> find(String tokenHash);
    boolean revoke(String tokenHash, Instant now);
    int revokeAll(String userId, Instant now);
    int countActive(Instant now);
}
