package io.github.regalpine.agentforge.security.iam;

import io.github.regalpine.agentforge.security.*;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.*;

/**
 * Authentication provider backed by the built-in IAM system.
 * <p>
 * Supports HTTP Basic authentication ({@code Basic base64(user:pass)}).
 * The provider looks up the user from the {@link UserStore}, verifies
 * the password via {@link PasswordHasher}, resolves roles from the
 * {@link RoleStore}, and constructs a full {@link SecurityContext}.
 * <p>
 * The tenant context is resolved from:
 * <ol>
 *   <li>The configured default tenant (constructor parameter)</li>
 *   <li>Or the first tenant-scoped role assignment found for the user</li>
 * </ol>
 */
public class BuiltInAuthenticationProvider
    implements AuthenticationProvider {

    private final UserStore userStore;
    private final RoleStore roleStore;
    private final TenantStore tenantStore;
    private final String defaultTenantId;

    public BuiltInAuthenticationProvider(
        UserStore userStore,
        RoleStore roleStore,
        TenantStore tenantStore,
        String defaultTenantId
    ) {
        this.userStore = Objects.requireNonNull(userStore);
        this.roleStore = Objects.requireNonNull(roleStore);
        this.tenantStore = Objects.requireNonNull(tenantStore);
        this.defaultTenantId = defaultTenantId != null
            ? defaultTenantId : "default";
    }

    public BuiltInAuthenticationProvider(
        UserStore userStore,
        RoleStore roleStore,
        TenantStore tenantStore
    ) {
        this(userStore, roleStore, tenantStore, "default");
    }

    @Override
    public AuthenticationResult authenticate(
        AuthenticationRequest request
    ) {
        String header = request.authorizationHeader();

        if (header == null || header.isBlank()) {
            return new AuthenticationFailure(
                "AF-IAM-001",
                "Missing authorization header"
            );
        }

        if (!header.startsWith("Basic ")) {
            return new AuthenticationFailure(
                "AF-IAM-002",
                "Expected Basic authentication scheme"
            );
        }

        String encoded = header.substring(6);
        String decoded;
        try {
            decoded = new String(
                Base64.getDecoder().decode(encoded),
                StandardCharsets.UTF_8
            );
        } catch (IllegalArgumentException e) {
            return new AuthenticationFailure(
                "AF-IAM-003",
                "Invalid Basic auth encoding"
            );
        }

        int colonIndex = decoded.indexOf(':');
        if (colonIndex < 0) {
            return new AuthenticationFailure(
                "AF-IAM-004",
                "Invalid Basic auth format"
            );
        }

        String username = decoded.substring(0, colonIndex);
        String password = decoded.substring(colonIndex + 1);

        Optional<User> userOpt =
            userStore.findByUsername(username);
        if (userOpt.isEmpty()) {
            return new AuthenticationFailure(
                "AF-IAM-005",
                "Invalid credentials"
            );
        }

        User user = userOpt.get();

        if (user.status() != User.UserStatus.ACTIVE) {
            return new AuthenticationFailure(
                "AF-IAM-006",
                "User account is not active"
            );
        }

        if (!user.hasBuiltInCredentials()) {
            return new AuthenticationFailure(
                "AF-IAM-007",
                "User has no built-in credentials"
            );
        }

        if (!PasswordHasher.verify(
            password, user.passwordHash()
        )) {
            return new AuthenticationFailure(
                "AF-IAM-005",
                "Invalid credentials"
            );
        }

        return buildSuccess(user, defaultTenantId);
    }

    /**
     * Authenticate a user with an explicit tenant context.
     *
     * @param username the username
     * @param password the plaintext password
     * @param tenantId the target tenant
     * @return the authentication result
     */
    public AuthenticationResult authenticate(
        String username,
        String password,
        String tenantId
    ) {
        Optional<User> userOpt =
            userStore.findByUsername(username);
        if (userOpt.isEmpty()) {
            return new AuthenticationFailure(
                "AF-IAM-005",
                "Invalid credentials"
            );
        }

        User user = userOpt.get();

        if (user.status() != User.UserStatus.ACTIVE) {
            return new AuthenticationFailure(
                "AF-IAM-006",
                "User account is not active"
            );
        }

        if (!user.hasBuiltInCredentials()) {
            return new AuthenticationFailure(
                "AF-IAM-007",
                "User has no built-in credentials"
            );
        }

        if (!PasswordHasher.verify(
            password, user.passwordHash()
        )) {
            return new AuthenticationFailure(
                "AF-IAM-005",
                "Invalid credentials"
            );
        }

        String resolvedTenant = tenantId != null
            ? tenantId : defaultTenantId;
        return buildSuccess(user, resolvedTenant);
    }

    private AuthenticationSuccess buildSuccess(
        User user, String tenantId
    ) {
        Set<String> permissions =
            resolvePermissions(user.userId(), tenantId);

        Set<String> roleNames =
            resolveRoleNames(user.userId(), tenantId);

        Map<String, Object> claims = new HashMap<>();
        claims.put("tenant", tenantId);
        claims.put("userType", user.type().name());
        claims.putAll(user.attributes());

        Identity identity = new Identity(
            user.userId(),
            "agentforge-iam",
            roleNames,
            permissions,
            Map.copyOf(claims)
        );

        TenantContext tenant = new TenantContext(
            tenantId,
            user.userId(),
            permissions
        );

        return new AuthenticationSuccess(
            new SecurityContext(
                identity, tenant, Instant.now()
            )
        );
    }

    private Set<String> resolvePermissions(
        String userId, String tenantId
    ) {
        Set<String> permissions = new HashSet<>();
        Set<RoleAssignment> assignments =
            roleStore.getAssignmentsForUserInTenant(
                userId, tenantId
            );

        for (RoleAssignment assignment : assignments) {
            roleStore.findRoleById(assignment.roleId())
                .ifPresent(role ->
                    permissions.addAll(role.permissions())
                );
        }

        return Set.copyOf(permissions);
    }

    private Set<String> resolveRoleNames(
        String userId, String tenantId
    ) {
        Set<String> names = new HashSet<>();
        Set<RoleAssignment> assignments =
            roleStore.getAssignmentsForUserInTenant(
                userId, tenantId
            );

        for (RoleAssignment assignment : assignments) {
            roleStore.findRoleById(assignment.roleId())
                .ifPresent(role -> names.add(role.name()));
        }

        return Set.copyOf(names);
    }
}
