package io.github.regalpine.agentforge.security.iam;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory {@link UserStore} for development and testing.
 * <p>
 * Not suitable for production use — data does not survive restarts.
 */
public class InMemoryUserStore implements UserStore {

    private final Map<String, User> usersById =
        new ConcurrentHashMap<>();

    private final Map<String, String> usernameIndex =
        new ConcurrentHashMap<>();

    @Override
    public void save(User user) {
        usersById.put(user.userId(), user);
        usernameIndex.put(user.username(), user.userId());
    }

    @Override
    public Optional<User> findById(String userId) {
        return Optional.ofNullable(usersById.get(userId));
    }

    @Override
    public Optional<User> findByUsername(String username) {
        String userId = usernameIndex.get(username);
        if (userId == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(usersById.get(userId));
    }

    @Override
    public Optional<User> findByEmail(String email) {
        if (email == null || email.isBlank()) {
            return Optional.empty();
        }
        return usersById.values().stream()
            .filter(u -> email.equals(u.email()))
            .findFirst();
    }

    @Override
    public Optional<User> findByExternalSubject(
        String externalSubject, String externalIssuer
    ) {
        if (externalSubject == null || externalIssuer == null) {
            return Optional.empty();
        }
        return usersById.values().stream()
            .filter(u ->
                externalSubject.equals(u.externalSubject())
                    && externalIssuer.equals(u.externalIssuer()))
            .findFirst();
    }

    @Override
    public List<User> findAll() {
        return List.copyOf(usersById.values());
    }

    @Override
    public boolean delete(String userId) {
        User removed = usersById.remove(userId);
        if (removed != null) {
            usernameIndex.remove(removed.username());
        }
        return removed != null;
    }

    /**
     * Get the count of stored users.
     */
    public int size() {
        return usersById.size();
    }
}
