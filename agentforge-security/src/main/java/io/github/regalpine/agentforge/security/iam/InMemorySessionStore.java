package io.github.regalpine.agentforge.security.iam;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/** 仅用于显式 demo 和会话存储契约测试。 */
public final class InMemorySessionStore implements SessionStore {
    private final Map<String, SessionRecord> sessions = new HashMap<>();

    @Override public synchronized void create(SessionRecord session) {
        if (sessions.putIfAbsent(session.tokenHash(), session) != null) {
            throw new IllegalStateException("Session hash already exists");
        }
    }
    @Override public synchronized Optional<SessionRecord> find(String hash) {
        return Optional.ofNullable(sessions.get(hash));
    }
    @Override public synchronized boolean revoke(String hash, Instant now) {
        SessionRecord session = sessions.get(hash);
        if (session == null || session.revokedAt() != null) return false;
        sessions.put(hash, revoked(session, now));
        return true;
    }
    @Override public synchronized int revokeAll(String userId, Instant now) {
        int count = 0;
        for (var entry : sessions.entrySet()) {
            if (entry.getValue().userId().equals(userId) && entry.getValue().activeAt(now)) {
                entry.setValue(revoked(entry.getValue(), now));
                count++;
            }
        }
        return count;
    }
    @Override public synchronized int countActive(Instant now) {
        return (int) sessions.values().stream().filter(s -> s.activeAt(now)).count();
    }
    private SessionRecord revoked(SessionRecord s, Instant now) {
        return new SessionRecord(s.tokenHash(), s.userId(), s.tenantId(), s.createdAt(), s.expiresAt(), now);
    }
}
