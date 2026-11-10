package io.github.regalpine.agentforge.security.iam;

import io.github.regalpine.agentforge.security.*;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.Clock;
import java.time.Duration;
import java.security.MessageDigest;
import io.github.regalpine.agentforge.security.iam.SessionStore.SessionRecord;
import java.util.*;


/**
 * Session token provider that issues and manages session tokens
 * for authenticated users.
 * <p>
 * After a user authenticates via {@link BuiltInAuthenticationProvider}
 * or {@link ExternalIamAuthenticationProvider}, this provider generates
 * a session token that can be used for subsequent Bearer authentication.
 * <p>
 * This enables the login flow:
 * <pre>
 *   1. POST /api/auth/login (Basic auth) → session token
 *   2. All subsequent requests: Authorization: Bearer {session-token}
 * </pre>
 */
public class SessionTokenProvider {

    private static final SecureRandom RANDOM = new SecureRandom();
    private static final int TOKEN_BYTES = 32;
    private static final String HMAC_ALGORITHM = "HmacSHA256";

    private final SessionStore sessions;
    private final Clock clock;
    private final Duration lifetime;

    private final UserStore userStore;
    private final RoleStore roleStore;
    private final TenantStore tenantStore;
    private final String defaultTenantId;
    private final byte[] signingKey;

    public SessionTokenProvider(
        UserStore userStore,
        RoleStore roleStore,
        TenantStore tenantStore,
        String defaultTenantId
    ) {
        this(userStore, roleStore, tenantStore, defaultTenantId,
            new InMemorySessionStore(), randomKey(), Duration.ofMinutes(60), Clock.systemUTC());
    }

    public SessionTokenProvider(UserStore userStore, RoleStore roleStore, TenantStore tenantStore,
                                String defaultTenantId, SessionStore sessions, byte[] signingKey,
                                Duration lifetime, Clock clock) {
        this.sessions = Objects.requireNonNull(sessions);
        this.clock = Objects.requireNonNull(clock);
        this.lifetime = Objects.requireNonNull(lifetime);
        if (lifetime.isNegative() || lifetime.isZero()) throw new IllegalArgumentException("Session lifetime must be positive");
        if (signingKey == null || signingKey.length < 32) throw new IllegalArgumentException("Session signing key must be at least 32 bytes");
        this.signingKey = signingKey.clone();
        this.userStore = Objects.requireNonNull(userStore);
        this.roleStore = Objects.requireNonNull(roleStore);
        this.tenantStore = Objects.requireNonNull(tenantStore);
        this.defaultTenantId = defaultTenantId != null
            ? defaultTenantId : "default";
        // Generate a random HMAC signing key for session token integrity
    }

    public static byte[] randomKey() {
        byte[] key = new byte[32];
        RANDOM.nextBytes(key);
        return key;
    }

    /**
     * Create a session token for an authenticated user.
     * <p>
     * The token is signed with HMAC-SHA256 to prevent tampering.
     * Format: {@code <random-part>.<hmac-signature>}
     *
     * @param userId the authenticated user's ID
     * @param tenantId the tenant context
     * @return the signed session token string
     */
    public String createSession(String userId, String tenantId) {
        byte[] tokenBytes = new byte[TOKEN_BYTES];
        RANDOM.nextBytes(tokenBytes);
        String randomPart = Base64.getUrlEncoder()
            .withoutPadding()
            .encodeToString(tokenBytes);

        String signature = hmacSign(randomPart);
        String token = randomPart + "." + signature;

        String resolvedTenant = tenantId != null
            ? tenantId : defaultTenantId;

        if (resolvedTenant.isBlank() || tenantStore.findById(resolvedTenant)
                .filter(t -> t.status() == Tenant.TenantStatus.ACTIVE).isEmpty()) {
            throw new SecurityException("Tenant is not active");
        }
        if (userStore.findById(userId).filter(u -> u.status() == User.UserStatus.ACTIVE).isEmpty()) {
            throw new SecurityException("User is not active");
        }
        Instant now = clock.instant();
        sessions.create(new SessionRecord(tokenHash(token), userId, resolvedTenant,
            now, now.plus(lifetime), null));

        return token;
    }

    /**
     * Validate a session token and return the associated identity.
     * <p>
     * Verifies the HMAC-SHA256 signature before looking up the session.
     *
     * @param token the session token
     * @return the authentication result
     */
    public AuthenticationResult validate(String token) {
        if (token == null || token.isBlank() || token.length() > 256) {
            return new AuthenticationFailure(
                "AF-IAM-030", "Missing session token"
            );
        }

        // Verify HMAC signature before lookup
        int dotIndex = token.lastIndexOf('.');
        if (dotIndex < 0 || dotIndex >= token.length() - 1) {
            return new AuthenticationFailure(
                "AF-IAM-034", "Malformed session token"
            );
        }
        String randomPart = token.substring(0, dotIndex);
        String providedSig = token.substring(dotIndex + 1);
        String expectedSig = hmacSign(randomPart);
        if (!constantTimeEquals(expectedSig, providedSig)) {
            return new AuthenticationFailure(
                "AF-IAM-035", "Invalid session token signature"
            );
        }

        SessionRecord record = sessions.find(tokenHash(token)).orElse(null);
        if (record == null || !record.activeAt(clock.instant())) {
            return new AuthenticationFailure(
                "AF-IAM-031", "Invalid or expired session"
            );
        }

        if (tenantStore.findById(record.tenantId()).filter(t -> t.status() == Tenant.TenantStatus.ACTIVE).isEmpty()) {
            sessions.revoke(record.tokenHash(), clock.instant());
            return new AuthenticationFailure("AF-IAM-036", "Tenant is not active");
        }
        Optional<User> userOpt =
            userStore.findById(record.userId());
        if (userOpt.isEmpty()) {
            sessions.revoke(record.tokenHash(), clock.instant());
            return new AuthenticationFailure(
                "AF-IAM-032", "User no longer exists"
            );
        }

        User user = userOpt.get();
        if (user.status() != User.UserStatus.ACTIVE) {
            sessions.revoke(record.tokenHash(), clock.instant());
            return new AuthenticationFailure(
                "AF-IAM-033", "User account is not active"
            );
        }

        Set<String> permissions = new HashSet<>();
        Set<String> roleNames = new HashSet<>();
        Set<RoleAssignment> assignments =
            roleStore.getAssignmentsForUserInTenant(
                user.userId(), record.tenantId()
            );
        for (RoleAssignment a : assignments) {
            roleStore.findRoleById(a.roleId())
                .filter(role -> role.isGlobal() || record.tenantId().equals(role.tenantId()))
                .ifPresent(role -> {
                    permissions.addAll(role.permissions());
                    roleNames.add(role.name());
                });
        }

        Map<String, Object> claims = new HashMap<>(user.attributes());
        claims.put("tenant", record.tenantId());
        claims.put("userType", user.type().name());
        claims.put("session", true);


        Identity identity = new Identity(
            user.userId(),
            "agentforge-session",
            roleNames,
            Set.copyOf(permissions),
            Map.copyOf(claims)
        );

        TenantContext tenant = new TenantContext(
            record.tenantId(),
            user.userId(),
            Set.copyOf(permissions)
        );

        return new AuthenticationSuccess(
            new SecurityContext(identity, tenant, clock.instant())
        );
    }

    /**
     * Invalidate a session token.
     *
     * @param token the session token to invalidate
     * @return true if the session existed and was removed
     */
    public boolean invalidate(String token) {
        return token != null && sessions.revoke(tokenHash(token), clock.instant());
    }

    /**
     * Invalidate all sessions for a user.
     *
     * @param userId the user ID
     * @return the number of sessions invalidated
     */
    public int invalidateAll(String userId) {
        return sessions.revokeAll(userId, clock.instant());
    }

    /**
     * Get the number of active sessions.
     */
    public int activeSessionCount() {
        return sessions.countActive(clock.instant());
    }

    private static String tokenHash(String token) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                .digest(token.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 unavailable", e);
        }
    }

    /**
     * Compute HMAC-SHA256 signature for the given data.
     */
    private String hmacSign(String data) {
        try {
            Mac mac = Mac.getInstance(HMAC_ALGORITHM);
            mac.init(new SecretKeySpec(signingKey, HMAC_ALGORITHM));
            byte[] hash = mac.doFinal(
                data.getBytes(StandardCharsets.UTF_8)
            );
            return Base64.getUrlEncoder()
                .withoutPadding()
                .encodeToString(hash);
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            throw new IllegalStateException(
                "HMAC-SHA256 signing failed", e
            );
        }
    }

    /**
     * Constant-time string comparison to prevent timing attacks.
     */
    private static boolean constantTimeEquals(String a, String b) {
        if (a.length() != b.length()) return false;
        int result = 0;
        for (int i = 0; i < a.length(); i++) {
            result |= a.charAt(i) ^ b.charAt(i);
        }
        return result == 0;
    }
}
