package io.github.regalpine.agentforge.security.iam;

import io.github.regalpine.agentforge.security.*;

import java.time.Instant;
import java.util.*;

/**
 * Authentication provider that delegates to an external IAM system.
 * <p>
 * This provider handles Bearer token authentication by delegating
 * token validation to an {@link ExternalIamAdapter}. It supports:
 * <ul>
 *   <li>Automatic user provisioning from external identities</li>
 *   <li>Mapping external roles/permissions to local roles</li>
 *   <li>Tenant resolution from token claims</li>
 * </ul>
 * <p>
 * When a valid external identity is presented for the first time,
 * the provider can auto-provision a local {@link User} record
 * (if auto-provisioning is enabled).
 */
public class ExternalIamAuthenticationProvider
    implements AuthenticationProvider {

    private final ExternalIamAdapter adapter;
    private final UserStore userStore;
    private final RoleStore roleStore;
    private final TenantStore tenantStore;
    private final String defaultTenantId;
    private final String tenantClaim;
    private final boolean autoProvision;

    private ExternalIamAuthenticationProvider(Builder builder) {
        this.adapter = Objects.requireNonNull(builder.adapter);
        this.userStore = Objects.requireNonNull(builder.userStore);
        this.roleStore = Objects.requireNonNull(builder.roleStore);
        this.tenantStore = Objects.requireNonNull(builder.tenantStore);
        this.defaultTenantId = builder.defaultTenantId != null
            ? builder.defaultTenantId : "default";
        this.tenantClaim = builder.tenantClaim != null
            ? builder.tenantClaim : "tenant";
        this.autoProvision = builder.autoProvision;
    }

    @Override
    public AuthenticationResult authenticate(
        AuthenticationRequest request
    ) {
        String header = request.authorizationHeader();

        if (header == null || header.isBlank()) {
            return new AuthenticationFailure(
                "AF-IAM-010",
                "Missing authorization header"
            );
        }

        if (!header.startsWith("Bearer ")) {
            return new AuthenticationFailure(
                "AF-IAM-011",
                "Expected Bearer authentication scheme"
            );
        }

        String token = header.substring(7);

        Optional<ExternalIamAdapter.ExternalIdentity> extOpt =
            adapter.validate(token);

        if (extOpt.isEmpty()) {
            return new AuthenticationFailure(
                "AF-IAM-012",
                "External token validation failed"
            );
        }

        ExternalIamAdapter.ExternalIdentity ext = extOpt.get();

        User user = findOrProvisionUser(ext);
        if (user == null) {
            return new AuthenticationFailure(
                "AF-IAM-013",
                "No local user mapped for external identity"
            );
        }

        if (user.status() != User.UserStatus.ACTIVE) {
            return new AuthenticationFailure(
                "AF-IAM-014",
                "User account is not active"
            );
        }

        String tenantId = resolveTenant(ext);

        return buildSuccess(user, ext, tenantId);
    }

    private User findOrProvisionUser(
        ExternalIamAdapter.ExternalIdentity ext
    ) {
        Optional<User> existing =
            userStore.findByExternalSubject(
                ext.subject(), ext.issuer()
            );

        if (existing.isPresent()) {
            return existing.get();
        }

        if (!autoProvision) {
            return null;
        }

        String userId = "ext-" + ext.subject();
        String username = ext.email() != null
            ? ext.email() : ext.subject();

        User newUser = new User(
            userId,
            username,
            ext.displayName() != null
                ? ext.displayName() : username,
            ext.email(),
            User.UserType.HUMAN,
            User.UserStatus.ACTIVE,
            null,
            ext.subject(),
            ext.issuer(),
            ext.externalRoles(),
            Map.of(),
            Instant.now(),
            Instant.now()
        );

        userStore.save(newUser);
        return newUser;
    }

    private String resolveTenant(
        ExternalIamAdapter.ExternalIdentity ext
    ) {
        Object claim = ext.claims().get(tenantClaim);
        if (claim instanceof String t && !t.isBlank()) {
            if (tenantStore.findById(t).isPresent()) {
                return t;
            }
        }
        return defaultTenantId;
    }

    private AuthenticationSuccess buildSuccess(
        User user,
        ExternalIamAdapter.ExternalIdentity ext,
        String tenantId
    ) {
        Set<String> permissions =
            resolvePermissions(user.userId(), tenantId, ext);

        Set<String> roleNames =
            resolveRoleNames(user.userId(), tenantId);

        Map<String, Object> claims = new HashMap<>();
        claims.put("tenant", tenantId);
        claims.put("issuer", ext.issuer());
        claims.put("externalSubject", ext.subject());
        claims.put("userType", user.type().name());

        Identity identity = new Identity(
            user.userId(),
            ext.issuer(),
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
        String userId,
        String tenantId,
        ExternalIamAdapter.ExternalIdentity ext
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

        permissions.addAll(ext.externalPermissions());

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
                .ifPresent(role ->
                    names.add(role.name())
                );
        }
        return Set.copyOf(names);
    }

    /**
     * Create a new builder.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for {@link ExternalIamAuthenticationProvider}.
     */
    public static final class Builder {
        private ExternalIamAdapter adapter;
        private UserStore userStore;
        private RoleStore roleStore;
        private TenantStore tenantStore;
        private String defaultTenantId;
        private String tenantClaim;
        private boolean autoProvision = true;

        public Builder adapter(ExternalIamAdapter adapter) {
            this.adapter = adapter;
            return this;
        }

        public Builder userStore(UserStore userStore) {
            this.userStore = userStore;
            return this;
        }

        public Builder roleStore(RoleStore roleStore) {
            this.roleStore = roleStore;
            return this;
        }

        public Builder tenantStore(TenantStore tenantStore) {
            this.tenantStore = tenantStore;
            return this;
        }

        public Builder defaultTenantId(String defaultTenantId) {
            this.defaultTenantId = defaultTenantId;
            return this;
        }

        public Builder tenantClaim(String tenantClaim) {
            this.tenantClaim = tenantClaim;
            return this;
        }

        public Builder autoProvision(boolean autoProvision) {
            this.autoProvision = autoProvision;
            return this;
        }

        public ExternalIamAuthenticationProvider build() {
            return new ExternalIamAuthenticationProvider(this);
        }
    }
}
