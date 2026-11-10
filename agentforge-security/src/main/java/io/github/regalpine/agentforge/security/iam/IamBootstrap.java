package io.github.regalpine.agentforge.security.iam;

import java.util.Objects;
import java.util.Set;

/**
 * Bootstraps the built-in IAM system with default data.
 * <p>
 * On first startup, creates:
 * <ul>
 *   <li>Default tenant ("default")</li>
 *   <li>System administrator role</li>
 *   <li>Default admin user ("admin" / configurable password)</li>
 *   <li>Standard viewer role</li>
 * </ul>
 * <p>
 * Bootstrap is idempotent — it skips entities that already exist.
 * In production, the default admin password should be changed
 * immediately after first login.
 */
public class IamBootstrap {

    private final TenantStore tenantStore;
    private final UserStore userStore;
    private final RoleStore roleStore;

    /**
     * Default admin username.
     */
    public static final String DEFAULT_ADMIN_USERNAME = "admin";

    /**
     * Default tenant ID.
     */
    public static final String DEFAULT_TENANT_ID = "default";

    /**
     * System administrator role ID.
     */
    public static final String ROLE_SYS_ADMIN = "sys-admin";

    /**
     * Tenant administrator role ID.
     */
    public static final String ROLE_TENANT_ADMIN = "tenant-admin";

    /**
     * Viewer role ID.
     */
    public static final String ROLE_VIEWER = "viewer";

    public IamBootstrap(
        TenantStore tenantStore,
        UserStore userStore,
        RoleStore roleStore
    ) {
        this.tenantStore = Objects.requireNonNull(tenantStore);
        this.userStore = Objects.requireNonNull(userStore);
        this.roleStore = Objects.requireNonNull(roleStore);
    }

    /**
     * Execute the bootstrap process with a specified admin password.
     * <p>
     * The password must be provided explicitly — there is no default.
     * In production, the password should come from the
     * {@code AGENTFORGE_ADMIN_PASSWORD} environment variable.
     *
     * @param adminPassword the initial admin password (must not be null or blank)
     * @return the bootstrap result
     * @throws IllegalArgumentException if adminPassword is blank
     */
    public BootstrapResult execute(String adminPassword) {
        Objects.requireNonNull(
            adminPassword, "adminPassword cannot be null"
        );
        if (adminPassword.isBlank()) {
            throw new IllegalArgumentException(
                "adminPassword must not be blank. "
                + "Set the AGENTFORGE_ADMIN_PASSWORD environment variable."
            );
        }

        int created = 0;

        // 1. Create default tenant
        if (tenantStore.findById(DEFAULT_TENANT_ID).isEmpty()) {
            tenantStore.save(
                Tenant.create(
                    DEFAULT_TENANT_ID,
                    "default",
                    "Default Tenant"
                )
            );
            created++;
        }

        // 2. Create system admin role (global)
        if (roleStore.findRoleById(ROLE_SYS_ADMIN).isEmpty()) {
            roleStore.saveRole(Role.global(
                ROLE_SYS_ADMIN,
                "System Administrator",
                Set.of(
                    "admin:system",
                    "admin:tenant",
                    "admin:provider",
                    "graph:read",
                    "graph:create",
                    "graph:update",
                    "graph:publish",
                    "graph:delete",
                    "execution:read",
                    "execution:create",
                    "execution:pause",
                    "execution:resume",
                    "execution:cancel",
                    "capability:invoke",
                    "provider:read"
                )
            ));
            created++;
        }

        // 3. Create tenant admin role
        if (roleStore.findRoleById(ROLE_TENANT_ADMIN).isEmpty()) {
            roleStore.saveRole(Role.tenantRole(
                ROLE_TENANT_ADMIN,
                "Tenant Administrator",
                DEFAULT_TENANT_ID,
                Set.of(
                    "graph:read",
                    "graph:create",
                    "graph:update",
                    "graph:publish",
                    "execution:read",
                    "execution:create",
                    "execution:pause",
                    "execution:resume",
                    "execution:cancel",
                    "capability:invoke",
                    "provider:read"
                )
            ));
            created++;
        }

        // 4. Create viewer role
        if (roleStore.findRoleById(ROLE_VIEWER).isEmpty()) {
            roleStore.saveRole(Role.tenantRole(
                ROLE_VIEWER,
                "Viewer",
                DEFAULT_TENANT_ID,
                Set.of(
                    "graph:read",
                    "execution:read",
                    "provider:read"
                )
            ));
            created++;
        }

        // 5. Create default admin user
        if (userStore.findByUsername(DEFAULT_ADMIN_USERNAME)
            .isEmpty()) {

            String passwordHash =
                PasswordHasher.hash(adminPassword);

            User admin = new User(
                "user-admin",
                DEFAULT_ADMIN_USERNAME,
                "System Administrator",
                "admin@agentforge.local",
                User.UserType.HUMAN,
                User.UserStatus.ACTIVE,
                passwordHash,
                null, null,
                Set.of(),
                java.util.Map.of(
                    "bootstrap", "true",
                    "passwordChangeRequired", "true"
                ),
                java.time.Instant.now(),
                java.time.Instant.now()
            );

            userStore.save(admin);
            created++;

            // Assign sys-admin role
            roleStore.assignRole(
                RoleAssignment.global(
                    admin.userId(), ROLE_SYS_ADMIN
                )
            );
            roleStore.assignRole(
                RoleAssignment.of(
                    admin.userId(),
                    ROLE_TENANT_ADMIN,
                    DEFAULT_TENANT_ID
                )
            );
        }

        return new BootstrapResult(
            created,
            tenantStore.findById(DEFAULT_TENANT_ID).orElseThrow(),
            userStore.findByUsername(DEFAULT_ADMIN_USERNAME)
                .orElseThrow()
        );
    }

    /**
     * Result of the bootstrap process.
     */
    public record BootstrapResult(
        int entitiesCreated,
        Tenant defaultTenant,
        User adminUser
    ) {

        /**
         * Whether this was a fresh bootstrap (first run).
         */
        public boolean isFirstRun() {
            return entitiesCreated > 0;
        }
    }
}
