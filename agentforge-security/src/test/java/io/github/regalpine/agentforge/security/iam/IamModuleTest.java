package io.github.regalpine.agentforge.security.iam;

import io.github.regalpine.agentforge.security.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class IamModuleTest {

    private InMemoryTenantStore tenantStore;
    private InMemoryUserStore userStore;
    private InMemoryRoleStore roleStore;

    @BeforeEach
    void setUp() {
        tenantStore = new InMemoryTenantStore();
        userStore = new InMemoryUserStore();
        roleStore = new InMemoryRoleStore();
    }

    // ── Domain Model Tests ──

    @Nested
    class TenantModelTest {

        @Test
        void shouldCreateTenantWithMinimalConfig() {
            Tenant tenant = Tenant.create("t1", "acme");

            assertEquals("t1", tenant.tenantId());
            assertEquals("acme", tenant.name());
            assertEquals(Tenant.TenantStatus.ACTIVE, tenant.status());
            assertNotNull(tenant.createdAt());
        }

        @Test
        void shouldCreateTenantWithDisplayName() {
            Tenant tenant = Tenant.create(
                "t1", "acme", "ACME Corp"
            );

            assertEquals("ACME Corp", tenant.displayName());
        }

        @Test
        void shouldRejectNullTenantId() {
            assertThrows(NullPointerException.class,
                () -> new Tenant(
                    null, "name", "display",
                    Tenant.TenantStatus.ACTIVE,
                    Map.of(), null, null
                )
            );
        }

        @Test
        void shouldDefensivelyCopySettings() {
            Map<String, String> settings = new HashMap<>();
            settings.put("key", "value");

            Tenant tenant = new Tenant(
                "t1", "name", "display",
                Tenant.TenantStatus.ACTIVE,
                settings, null, null
            );

            assertThrows(UnsupportedOperationException.class,
                () -> tenant.settings().put("x", "y")
            );
        }
    }

    @Nested
    class UserModelTest {

        @Test
        void shouldCreateHumanUser() {
            User user = User.human(
                "u1", "admin", "hash123"
            );

            assertEquals("u1", user.userId());
            assertEquals("admin", user.username());
            assertEquals(User.UserType.HUMAN, user.type());
            assertEquals(User.UserStatus.ACTIVE, user.status());
            assertTrue(user.hasBuiltInCredentials());
            assertFalse(user.hasExternalIdentity());
        }

        @Test
        void shouldCreateServiceAccount() {
            User user = User.serviceAccount(
                "sa1", "ci-bot"
            );

            assertEquals(User.UserType.SERVICE, user.type());
            assertFalse(user.hasBuiltInCredentials());
        }

        @Test
        void shouldCreateExternalUser() {
            User user = User.external(
                "u2", "oidc-user",
                "sub-123", "https://iam.example.com"
            );

            assertTrue(user.hasExternalIdentity());
            assertFalse(user.hasBuiltInCredentials());
            assertEquals("sub-123", user.externalSubject());
        }
    }

    @Nested
    class RoleModelTest {

        @Test
        void shouldCreateTenantRole() {
            Role role = Role.tenantRole(
                "r1", "admin", "t1",
                Set.of("graph:read", "graph:create")
            );

            assertEquals("r1", role.roleId());
            assertEquals("admin", role.name());
            assertEquals("t1", role.tenantId());
            assertFalse(role.isGlobal());
            assertEquals(2, role.permissions().size());
        }

        @Test
        void shouldCreateGlobalRole() {
            Role role = Role.global(
                "r2", "super-admin",
                Set.of("admin:system")
            );

            assertTrue(role.isGlobal());
            assertNull(role.tenantId());
        }
    }

    @Nested
    class RoleAssignmentTest {

        @Test
        void shouldCreateTenantScopedAssignment() {
            var assignment = RoleAssignment.of(
                "u1", "r1", "t1"
            );

            assertFalse(assignment.isGlobal());
            assertEquals("t1", assignment.tenantId());
        }

        @Test
        void shouldCreateGlobalAssignment() {
            var assignment = RoleAssignment.global(
                "u1", "r1"
            );

            assertTrue(assignment.isGlobal());
            assertNull(assignment.tenantId());
        }
    }

    // ── Store Tests ──

    @Nested
    class TenantStoreTest {

        @Test
        void shouldSaveAndFindTenant() {
            Tenant tenant = Tenant.create("t1", "acme");
            tenantStore.save(tenant);

            var found = tenantStore.findById("t1");
            assertTrue(found.isPresent());
            assertEquals("acme", found.get().name());
        }

        @Test
        void shouldFindByName() {
            tenantStore.save(Tenant.create("t1", "acme"));

            var found = tenantStore.findByName("acme");
            assertTrue(found.isPresent());
            assertEquals("t1", found.get().tenantId());
        }

        @Test
        void shouldReturnEmptyForUnknownTenant() {
            assertTrue(tenantStore.findById("unknown").isEmpty());
        }

        @Test
        void shouldDeleteTenant() {
            tenantStore.save(Tenant.create("t1", "acme"));
            assertTrue(tenantStore.delete("t1"));
            assertTrue(tenantStore.findById("t1").isEmpty());
        }

        @Test
        void shouldListAllTenants() {
            tenantStore.save(Tenant.create("t1", "a"));
            tenantStore.save(Tenant.create("t2", "b"));

            assertEquals(2, tenantStore.findAll().size());
        }
    }

    @Nested
    class UserStoreTest {

        @Test
        void shouldSaveAndFindUser() {
            User user = User.human("u1", "admin", "hash");
            userStore.save(user);

            assertTrue(userStore.findById("u1").isPresent());
            assertTrue(userStore.findByUsername("admin").isPresent());
        }

        @Test
        void shouldFindByExternalSubject() {
            User user = User.external(
                "u2", "oidc-user",
                "sub-123", "https://iam.example.com"
            );
            userStore.save(user);

            var found = userStore.findByExternalSubject(
                "sub-123", "https://iam.example.com"
            );
            assertTrue(found.isPresent());
            assertEquals("u2", found.get().userId());
        }

        @Test
        void shouldDeleteUser() {
            userStore.save(User.human("u1", "admin", "hash"));
            assertTrue(userStore.delete("u1"));
            assertTrue(userStore.findById("u1").isEmpty());
            assertTrue(userStore.findByUsername("admin").isEmpty());
        }
    }

    @Nested
    class RoleStoreTest {

        @Test
        void shouldSaveAndFindRole() {
            Role role = Role.tenantRole(
                "r1", "admin", "t1",
                Set.of("graph:read")
            );
            roleStore.saveRole(role);

            assertTrue(roleStore.findRoleById("r1").isPresent());
        }

        @Test
        void shouldFindRolesByTenant() {
            roleStore.saveRole(Role.tenantRole(
                "r1", "admin", "t1", Set.of("graph:read")
            ));
            roleStore.saveRole(Role.tenantRole(
                "r2", "viewer", "t1", Set.of("graph:read")
            ));
            roleStore.saveRole(Role.tenantRole(
                "r3", "admin", "t2", Set.of("graph:read")
            ));

            assertEquals(2, roleStore.findRolesByTenant("t1").size());
        }

        @Test
        void shouldAssignAndResolveRoles() {
            roleStore.saveRole(Role.tenantRole(
                "r1", "admin", "t1",
                Set.of("graph:read", "graph:create")
            ));
            roleStore.assignRole(
                RoleAssignment.of("u1", "r1", "t1")
            );

            var assignments =
                roleStore.getAssignmentsForUserInTenant("u1", "t1");
            assertEquals(1, assignments.size());
        }

        @Test
        void shouldIncludeGlobalAssignmentsInTenantQuery() {
            roleStore.saveRole(Role.global(
                "r-global", "super-admin",
                Set.of("admin:system")
            ));
            roleStore.saveRole(Role.tenantRole(
                "r1", "admin", "t1",
                Set.of("graph:read")
            ));

            roleStore.assignRole(
                RoleAssignment.global("u1", "r-global")
            );
            roleStore.assignRole(
                RoleAssignment.of("u1", "r1", "t1")
            );

            var assignments =
                roleStore.getAssignmentsForUserInTenant("u1", "t1");
            assertEquals(2, assignments.size());
        }

        @Test
        void shouldRemoveRoleAssignment() {
            roleStore.assignRole(
                RoleAssignment.of("u1", "r1", "t1")
            );
            assertTrue(roleStore.removeRoleAssignment(
                "u1", "r1", "t1"
            ));
            assertTrue(
                roleStore.getAssignmentsForUserInTenant(
                    "u1", "t1"
                ).isEmpty()
            );
        }
    }

    // ── Password Hasher Tests ──

    @Nested
    class PasswordHasherTest {

        @Test
        void shouldHashAndVerify() {
            String hash = PasswordHasher.hash("secret123");

            assertNotNull(hash);
            assertTrue(hash.contains(":"));
            assertTrue(PasswordHasher.verify("secret123", hash));
        }

        @Test
        void shouldRejectWrongPassword() {
            String hash = PasswordHasher.hash("secret123");

            assertFalse(
                PasswordHasher.verify("wrong", hash)
            );
        }

        @Test
        void shouldGenerateUniqueHashes() {
            String hash1 = PasswordHasher.hash("same");
            String hash2 = PasswordHasher.hash("same");

            assertNotEquals(hash1, hash2);

            assertTrue(PasswordHasher.verify("same", hash1));
            assertTrue(PasswordHasher.verify("same", hash2));
        }

        @Test
        void shouldHandleNullInputs() {
            assertFalse(PasswordHasher.verify(null, "hash"));
            assertFalse(PasswordHasher.verify("pass", null));
        }

        @Test
        void shouldRejectMalformedHash() {
            assertFalse(
                PasswordHasher.verify("pass", "no-colon")
            );
        }
    }

    // ── BuiltInAuthenticationProvider Tests ──

    @Nested
    class BuiltInAuthTest {

        private BuiltInAuthenticationProvider provider;

        @BeforeEach
        void setUp() {
            tenantStore.save(Tenant.create("t1", "acme"));

            String hash = PasswordHasher.hash("password123");
            userStore.save(User.human("u1", "admin", hash));

            roleStore.saveRole(Role.tenantRole(
                "r1", "admin", "t1",
                Set.of("graph:read", "graph:create",
                    "execution:create")
            ));
            roleStore.assignRole(
                RoleAssignment.of("u1", "r1", "t1")
            );

            provider = new BuiltInAuthenticationProvider(
                userStore, roleStore, tenantStore, "t1"
            );
        }

        @Test
        void shouldAuthenticateWithBasicAuth() {
            String credentials = Base64.getEncoder()
                .encodeToString("admin:password123".getBytes(
                    StandardCharsets.UTF_8
                ));

            var result = provider.authenticate(
                new AuthenticationRequest("Basic " + credentials)
            );

            assertInstanceOf(AuthenticationSuccess.class, result);
            var success = (AuthenticationSuccess) result;
            assertEquals(
                "u1",
                success.securityContext().identity().subjectId()
            );
            assertEquals(
                "t1",
                success.securityContext().tenant().tenantId()
            );
        }

        @Test
        void shouldResolvePermissionsFromRoles() {
            String credentials = Base64.getEncoder()
                .encodeToString("admin:password123".getBytes(
                    StandardCharsets.UTF_8
                ));

            var result = (AuthenticationSuccess)
                provider.authenticate(
                    new AuthenticationRequest(
                        "Basic " + credentials
                    )
                );

            var perms = result.securityContext()
                .identity().permissions();
            assertTrue(perms.contains("graph:read"));
            assertTrue(perms.contains("graph:create"));
            assertTrue(perms.contains("execution:create"));
        }

        @Test
        void shouldResolveRoleNames() {
            String credentials = Base64.getEncoder()
                .encodeToString("admin:password123".getBytes(
                    StandardCharsets.UTF_8
                ));

            var result = (AuthenticationSuccess)
                provider.authenticate(
                    new AuthenticationRequest(
                        "Basic " + credentials
                    )
                );

            var roles = result.securityContext()
                .identity().roles();
            assertTrue(roles.contains("admin"));
        }

        @Test
        void shouldRejectWrongPassword() {
            String credentials = Base64.getEncoder()
                .encodeToString("admin:wrong".getBytes(
                    StandardCharsets.UTF_8
                ));

            var result = provider.authenticate(
                new AuthenticationRequest("Basic " + credentials)
            );

            assertInstanceOf(AuthenticationFailure.class, result);
            assertEquals(
                "AF-IAM-005",
                ((AuthenticationFailure) result).code()
            );
        }

        @Test
        void shouldRejectUnknownUser() {
            String credentials = Base64.getEncoder()
                .encodeToString("unknown:pass".getBytes(
                    StandardCharsets.UTF_8
                ));

            var result = provider.authenticate(
                new AuthenticationRequest("Basic " + credentials)
            );

            assertInstanceOf(AuthenticationFailure.class, result);
        }

        @Test
        void shouldRejectMissingHeader() {
            var result = provider.authenticate(
                new AuthenticationRequest(null)
            );

            assertInstanceOf(AuthenticationFailure.class, result);
            assertEquals(
                "AF-IAM-001",
                ((AuthenticationFailure) result).code()
            );
        }

        @Test
        void shouldRejectNonBasicScheme() {
            var result = provider.authenticate(
                new AuthenticationRequest("Bearer token")
            );

            assertInstanceOf(AuthenticationFailure.class, result);
            assertEquals(
                "AF-IAM-002",
                ((AuthenticationFailure) result).code()
            );
        }

        @Test
        void shouldRejectDisabledUser() {
            String hash = PasswordHasher.hash("pass");
            User disabled = new User(
                "u2", "disabled-user", "Disabled", null,
                User.UserType.HUMAN, User.UserStatus.DISABLED,
                hash, null, null,
                Set.of(), Map.of(), null, null
            );
            userStore.save(disabled);

            String credentials = Base64.getEncoder()
                .encodeToString(
                    "disabled-user:pass".getBytes(
                        StandardCharsets.UTF_8
                    )
                );

            var result = provider.authenticate(
                new AuthenticationRequest("Basic " + credentials)
            );

            assertInstanceOf(AuthenticationFailure.class, result);
            assertEquals(
                "AF-IAM-006",
                ((AuthenticationFailure) result).code()
            );
        }

        @Test
        void shouldAuthenticateWithExplicitTenant() {
            String credentials = Base64.getEncoder()
                .encodeToString("admin:password123".getBytes(
                    StandardCharsets.UTF_8
                ));

            var result = provider.authenticate(
                "admin", "password123", "t1"
            );

            assertInstanceOf(AuthenticationSuccess.class, result);
        }
    }

    // ── ExternalIamAuthenticationProvider Tests ──

    @Nested
    class ExternalIamAuthTest {

        private ExternalIamAuthenticationProvider provider;

        @BeforeEach
        void setUp() {
            tenantStore.save(Tenant.create("t1", "acme"));
            tenantStore.save(Tenant.create("t-claim", "claim-tenant"));

            provider = ExternalIamAuthenticationProvider.builder()
                .adapter(new StubExternalIamAdapter())
                .userStore(userStore)
                .roleStore(roleStore)
                .tenantStore(tenantStore)
                .defaultTenantId("t1")
                .autoProvision(true)
                .build();
        }

        @Test
        void shouldAuthenticateWithBearerToken() {
            var result = provider.authenticate(
                new AuthenticationRequest("Bearer valid-token")
            );

            assertInstanceOf(AuthenticationSuccess.class, result);
            var success = (AuthenticationSuccess) result;
            assertEquals(
                "t1",
                success.securityContext().tenant().tenantId()
            );
        }

        @Test
        void shouldAutoProvisionUser() {
            provider.authenticate(
                new AuthenticationRequest("Bearer valid-token")
            );

            var user = userStore.findByExternalSubject(
                "ext-sub-001", "https://iam.example.com"
            );
            assertTrue(user.isPresent());
            assertEquals("ext-user@example.com", user.get().email());
        }

        @Test
        void shouldResolveTenantFromClaims() {
            var result = provider.authenticate(
                new AuthenticationRequest(
                    "Bearer tenant-token"
                )
            );

            assertInstanceOf(AuthenticationSuccess.class, result);
            var success = (AuthenticationSuccess) result;
            assertEquals(
                "t-claim",
                success.securityContext().tenant().tenantId()
            );
        }

        @Test
        void shouldRejectInvalidToken() {
            var result = provider.authenticate(
                new AuthenticationRequest("Bearer invalid")
            );

            assertInstanceOf(AuthenticationFailure.class, result);
            assertEquals(
                "AF-IAM-012",
                ((AuthenticationFailure) result).code()
            );
        }

        @Test
        void shouldRejectMissingBearer() {
            var result = provider.authenticate(
                new AuthenticationRequest(null)
            );

            assertInstanceOf(AuthenticationFailure.class, result);
            assertEquals(
                "AF-IAM-010",
                ((AuthenticationFailure) result).code()
            );
        }

        @Test
        void shouldRejectNonBearerScheme() {
            var result = provider.authenticate(
                new AuthenticationRequest("Basic xyz")
            );

            assertInstanceOf(AuthenticationFailure.class, result);
            assertEquals(
                "AF-IAM-011",
                ((AuthenticationFailure) result).code()
            );
        }

        @Test
        void shouldNotAutoProvisionWhenDisabled() {
            var strictProvider =
                ExternalIamAuthenticationProvider.builder()
                    .adapter(new StubExternalIamAdapter())
                    .userStore(userStore)
                    .roleStore(roleStore)
                    .tenantStore(tenantStore)
                    .defaultTenantId("t1")
                    .autoProvision(false)
                    .build();

            var result = strictProvider.authenticate(
                new AuthenticationRequest("Bearer valid-token")
            );

            assertInstanceOf(AuthenticationFailure.class, result);
            assertEquals(
                "AF-IAM-013",
                ((AuthenticationFailure) result).code()
            );
        }
    }

    // ── CompositeAuthenticationProvider Tests ──

    @Nested
    class CompositeAuthTest {

        @Test
        void shouldTryBuiltInFirst() {
            tenantStore.save(Tenant.create("t1", "acme"));
            String hash = PasswordHasher.hash("pass");
            userStore.save(User.human("u1", "admin", hash));

            var builtIn = new BuiltInAuthenticationProvider(
                userStore, roleStore, tenantStore, "t1"
            );
            var external = ExternalIamAuthenticationProvider.builder()
                .adapter(new StubExternalIamAdapter())
                .userStore(userStore)
                .roleStore(roleStore)
                .tenantStore(tenantStore)
                .defaultTenantId("t1")
                .build();

            var composite = new CompositeAuthenticationProvider(
                builtIn, external
            );

            String credentials = Base64.getEncoder()
                .encodeToString("admin:pass".getBytes(
                    StandardCharsets.UTF_8
                ));

            var result = composite.authenticate(
                new AuthenticationRequest("Basic " + credentials)
            );

            assertInstanceOf(AuthenticationSuccess.class, result);
        }

        @Test
        void shouldFallBackToExternal() {
            tenantStore.save(Tenant.create("t1", "acme"));

            var builtIn = new BuiltInAuthenticationProvider(
                userStore, roleStore, tenantStore, "t1"
            );
            var external = ExternalIamAuthenticationProvider.builder()
                .adapter(new StubExternalIamAdapter())
                .userStore(userStore)
                .roleStore(roleStore)
                .tenantStore(tenantStore)
                .defaultTenantId("t1")
                .build();

            var composite = new CompositeAuthenticationProvider(
                builtIn, external
            );

            var result = composite.authenticate(
                new AuthenticationRequest("Bearer valid-token")
            );

            assertInstanceOf(AuthenticationSuccess.class, result);
        }

        @Test
        void shouldReturnLastFailureWhenAllFail() {
            var builtIn = new BuiltInAuthenticationProvider(
                userStore, roleStore, tenantStore, "t1"
            );

            var composite = new CompositeAuthenticationProvider(
                builtIn
            );

            var result = composite.authenticate(
                new AuthenticationRequest("Basic invalid")
            );

            assertInstanceOf(AuthenticationFailure.class, result);
        }
    }

    // ── End-to-End IAM Flow Test ──

    @Nested
    class EndToEndIamTest {

        @Test
        void shouldCompleteFullIamLifecycle() {
            tenantStore.save(Tenant.create("t1", "acme"));
            tenantStore.save(Tenant.create("t2", "beta"));

            String adminHash = PasswordHasher.hash("admin-pass");
            userStore.save(User.human("u1", "admin", adminHash));
            userStore.save(
                User.serviceAccount("sa1", "ci-bot")
            );

            roleStore.saveRole(Role.tenantRole(
                "r-admin", "tenant-admin", "t1",
                Set.of("graph:read", "graph:create",
                    "graph:publish", "execution:create",
                    "execution:read")
            ));
            roleStore.saveRole(Role.tenantRole(
                "r-viewer", "viewer", "t1",
                Set.of("graph:read", "execution:read")
            ));
            roleStore.saveRole(Role.global(
                "r-sysadmin", "system-admin",
                Set.of("admin:system", "admin:tenant")
            ));

            roleStore.assignRole(
                RoleAssignment.of("u1", "r-admin", "t1")
            );
            roleStore.assignRole(
                RoleAssignment.global("u1", "r-sysadmin")
            );

            var provider = new BuiltInAuthenticationProvider(
                userStore, roleStore, tenantStore, "t1"
            );

            String credentials = Base64.getEncoder()
                .encodeToString("admin:admin-pass".getBytes(
                    StandardCharsets.UTF_8
                ));

            var result = (AuthenticationSuccess)
                provider.authenticate(
                    new AuthenticationRequest(
                        "Basic " + credentials
                    )
                );

            assertEquals("u1",
                result.securityContext().identity().subjectId());
            assertEquals("t1",
                result.securityContext().tenant().tenantId());

            var perms = result.securityContext()
                .identity().permissions();
            assertTrue(perms.contains("graph:read"));
            assertTrue(perms.contains("graph:publish"));
            assertTrue(perms.contains("admin:system"));

            var roles = result.securityContext()
                .identity().roles();
            assertTrue(roles.contains("tenant-admin"));
            assertTrue(roles.contains("system-admin"));
        }
    }

    // ── Stub External IAM Adapter ──

    static class StubExternalIamAdapter
        implements ExternalIamAdapter {

        @Override
        public Optional<ExternalIdentity> validate(String token) {
            if ("valid-token".equals(token)) {
                return Optional.of(new ExternalIdentity(
                    "ext-sub-001",
                    "https://iam.example.com",
                    "ext-user@example.com",
                    "External User",
                    Set.of("ext-admin"),
                    Set.of("graph:read"),
                    Map.of("tenant", "t1")
                ));
            }
            if ("tenant-token".equals(token)) {
                return Optional.of(new ExternalIdentity(
                    "ext-sub-002",
                    "https://iam.example.com",
                    "ext-user2@example.com",
                    "External User 2",
                    Set.of(),
                    Set.of(),
                    Map.of("tenant", "t-claim")
                ));
            }
            return Optional.empty();
        }

        @Override
        public String issuer() {
            return "https://iam.example.com";
        }
    }
}
