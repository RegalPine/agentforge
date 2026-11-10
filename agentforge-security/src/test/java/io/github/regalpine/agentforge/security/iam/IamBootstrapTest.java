package io.github.regalpine.agentforge.security.iam;

import io.github.regalpine.agentforge.security.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class IamBootstrapTest {

    private InMemoryTenantStore tenantStore;
    private InMemoryUserStore userStore;
    private InMemoryRoleStore roleStore;

    @BeforeEach
    void setUp() {
        tenantStore = new InMemoryTenantStore();
        userStore = new InMemoryUserStore();
        roleStore = new InMemoryRoleStore();
    }

    @Nested
    class BootstrapTest {

        @Test
        void shouldCreateDefaultDataOnFirstRun() {
            var bootstrap = new IamBootstrap(
                tenantStore, userStore, roleStore
            );

            var result = bootstrap.execute("secure-password");

            assertTrue(result.isFirstRun());
            assertTrue(result.entitiesCreated() > 0);
            assertEquals("default", result.defaultTenant().tenantId());
            assertEquals("admin", result.adminUser().username());
        }

        @Test
        void shouldCreateDefaultTenant() {
            var bootstrap = new IamBootstrap(
                tenantStore, userStore, roleStore
            );
            bootstrap.execute("secure-password");

            var tenant = tenantStore.findById("default");
            assertTrue(tenant.isPresent());
            assertEquals("Default Tenant", tenant.get().displayName());
        }

        @Test
        void shouldCreateAdminUserWithPassword() {
            var bootstrap = new IamBootstrap(
                tenantStore, userStore, roleStore
            );
            bootstrap.execute("my-secret");

            var admin = userStore.findByUsername("admin");
            assertTrue(admin.isPresent());
            assertTrue(admin.get().hasBuiltInCredentials());
            assertTrue(
                PasswordHasher.verify("my-secret", admin.get().passwordHash())
            );
        }

        @Test
        void shouldRejectBlankPassword() {
            var bootstrap = new IamBootstrap(
                tenantStore, userStore, roleStore
            );
            assertThrows(IllegalArgumentException.class, () ->
                bootstrap.execute("   ")
            );
        }

        @Test
        void shouldRejectEmptyPassword() {
            var bootstrap = new IamBootstrap(
                tenantStore, userStore, roleStore
            );
            assertThrows(IllegalArgumentException.class, () ->
                bootstrap.execute("")
            );
        }

        @Test
        void shouldAssignAdminRoles() {
            var bootstrap = new IamBootstrap(
                tenantStore, userStore, roleStore
            );
            bootstrap.execute("secure-password");

            var admin = userStore.findByUsername("admin").orElseThrow();
            var assignments = roleStore.getAssignmentsForUserInTenant(
                admin.userId(), "default"
            );

            assertTrue(assignments.size() >= 2);
        }

        @Test
        void shouldCreateStandardRoles() {
            var bootstrap = new IamBootstrap(
                tenantStore, userStore, roleStore
            );
            bootstrap.execute("secure-password");

            assertTrue(roleStore.findRoleById("sys-admin").isPresent());
            assertTrue(roleStore.findRoleById("tenant-admin").isPresent());
            assertTrue(roleStore.findRoleById("viewer").isPresent());
        }

        @Test
        void shouldBeIdempotent() {
            var bootstrap = new IamBootstrap(
                tenantStore, userStore, roleStore
            );

            var first = bootstrap.execute("secure-password");
            var second = bootstrap.execute("secure-password");

            assertTrue(first.isFirstRun());
            assertFalse(second.isFirstRun());
            assertEquals(0, second.entitiesCreated());
        }

        @Test
        void shouldPreserveExistingData() {
            tenantStore.save(Tenant.create("default", "existing"));
            userStore.save(
                User.human("u1", "admin", PasswordHasher.hash("original"))
            );

            var bootstrap = new IamBootstrap(
                tenantStore, userStore, roleStore
            );
            bootstrap.execute("new-password");

            var admin = userStore.findByUsername("admin").orElseThrow();
            assertTrue(
                PasswordHasher.verify("original", admin.passwordHash())
            );
            assertFalse(
                PasswordHasher.verify("new-password", admin.passwordHash())
            );
        }
    }

    @Nested
    class SessionTokenTest {

        private SessionTokenProvider sessionProvider;

        @BeforeEach
        void setUp() {
            var bootstrap = new IamBootstrap(
                tenantStore, userStore, roleStore
            );
            bootstrap.execute("test-password");

            sessionProvider = new SessionTokenProvider(
                userStore, roleStore, tenantStore, "default"
            );
        }

        @Test
        void shouldCreateAndValidateSession() {
            var admin = userStore.findByUsername("admin").orElseThrow();
            String token = sessionProvider.createSession(
                admin.userId(), "default"
            );

            assertNotNull(token);
            assertFalse(token.isBlank());

            var result = sessionProvider.validate(token);
            assertInstanceOf(AuthenticationSuccess.class, result);

            var success = (AuthenticationSuccess) result;
            assertEquals(
                admin.userId(),
                success.securityContext().identity().subjectId()
            );
            assertEquals(
                "default",
                success.securityContext().tenant().tenantId()
            );
        }

        @Test
        void shouldResolvePermissionsFromSession() {
            var admin = userStore.findByUsername("admin").orElseThrow();
            String token = sessionProvider.createSession(
                admin.userId(), "default"
            );

            var success = (AuthenticationSuccess)
                sessionProvider.validate(token);

            var perms = success.securityContext()
                .identity().permissions();
            assertTrue(perms.contains("admin:system"));
            assertTrue(perms.contains("graph:read"));
        }

        @Test
        void shouldRejectInvalidToken() {
            var result = sessionProvider.validate("invalid-token");
            assertInstanceOf(AuthenticationFailure.class, result);
        }

        @Test
        void shouldRejectNullToken() {
            var result = sessionProvider.validate(null);
            assertInstanceOf(AuthenticationFailure.class, result);
        }

        @Test
        void shouldInvalidateSession() {
            var admin = userStore.findByUsername("admin").orElseThrow();
            String token = sessionProvider.createSession(
                admin.userId(), "default"
            );

            assertTrue(sessionProvider.invalidate(token));

            var result = sessionProvider.validate(token);
            assertInstanceOf(AuthenticationFailure.class, result);
        }

        @Test
        void shouldInvalidateAllSessionsForUser() {
            var admin = userStore.findByUsername("admin").orElseThrow();
            String t1 = sessionProvider.createSession(
                admin.userId(), "default"
            );
            String t2 = sessionProvider.createSession(
                admin.userId(), "default"
            );

            assertEquals(2, sessionProvider.activeSessionCount());

            int count = sessionProvider.invalidateAll(admin.userId());
            assertEquals(2, count);
            assertEquals(0, sessionProvider.activeSessionCount());
        }

        @Test
        void shouldRejectSessionForDisabledUser() {
            var admin = userStore.findByUsername("admin").orElseThrow();
            String token = sessionProvider.createSession(
                admin.userId(), "default"
            );

            User disabled = new User(
                admin.userId(), admin.username(), admin.displayName(),
                admin.email(), admin.type(), User.UserStatus.DISABLED,
                admin.passwordHash(), admin.externalSubject(),
                admin.externalIssuer(), admin.globalRoles(),
                admin.attributes(), admin.createdAt(), admin.updatedAt()
            );
            userStore.save(disabled);

            var result = sessionProvider.validate(token);
            assertInstanceOf(AuthenticationFailure.class, result);
            assertEquals("AF-IAM-033", ((AuthenticationFailure) result).code());
        }

        @Test
        void shouldRejectTamperedToken() {
            var admin = userStore.findByUsername("admin").orElseThrow();
            String token = sessionProvider.createSession(
                admin.userId(), "default"
            );

            // Tamper with the token by modifying a character
            String tampered = token.substring(0, token.length() - 2) + "XX";

            var result = sessionProvider.validate(tampered);
            assertInstanceOf(AuthenticationFailure.class, result);
        }

        @Test
        void shouldRejectMalformedTokenWithoutSignature() {
            var result = sessionProvider.validate("no-signature-here");
            assertInstanceOf(AuthenticationFailure.class, result);
            assertEquals("AF-IAM-034", ((AuthenticationFailure) result).code());
        }

        @Test
        void shouldRejectTokenWithInvalidSignature() {
            var result = sessionProvider.validate("randompart.invalidsignature");
            assertInstanceOf(AuthenticationFailure.class, result);
            assertEquals("AF-IAM-035", ((AuthenticationFailure) result).code());
        }

        @Test
        void shouldProduceSignedTokenFormat() {
            var admin = userStore.findByUsername("admin").orElseThrow();
            String token = sessionProvider.createSession(
                admin.userId(), "default"
            );

            // Token should contain a dot separator (randomPart.signature)
            assertTrue(token.contains("."));
            String[] parts = token.split("\\.");
            assertEquals(2, parts.length);
            assertFalse(parts[0].isBlank());
            assertFalse(parts[1].isBlank());
        }
    }

    @Nested
    class LoginFlowTest {

        @Test
        void shouldCompleteFullLoginFlow() {
            String adminPassword = "secure-test-password";
            var bootstrap = new IamBootstrap(
                tenantStore, userStore, roleStore
            );
            bootstrap.execute(adminPassword);

            var authProvider = new BuiltInAuthenticationProvider(
                userStore, roleStore, tenantStore, "default"
            );
            var sessionProvider = new SessionTokenProvider(
                userStore, roleStore, tenantStore, "default"
            );

            // Step 1: Login with Basic auth
            String credentials = java.util.Base64.getEncoder()
                .encodeToString(
                    ("admin:" + adminPassword).getBytes(
                        java.nio.charset.StandardCharsets.UTF_8
                    )
                );

            var loginResult = authProvider.authenticate(
                new AuthenticationRequest("Basic " + credentials)
            );
            assertInstanceOf(AuthenticationSuccess.class, loginResult);

            // Step 2: Create session token
            var loginSuccess = (AuthenticationSuccess) loginResult;
            String sessionToken = sessionProvider.createSession(
                loginSuccess.securityContext().identity().subjectId(),
                loginSuccess.securityContext().tenant().tenantId()
            );

            // Step 3: Validate session token
            var sessionResult = sessionProvider.validate(sessionToken);
            assertInstanceOf(AuthenticationSuccess.class, sessionResult);

            var sessionSuccess = (AuthenticationSuccess) sessionResult;
            assertEquals(
                "user-admin",
                sessionSuccess.securityContext().identity().subjectId()
            );
            assertEquals(
                "default",
                sessionSuccess.securityContext().tenant().tenantId()
            );
            assertTrue(sessionSuccess.securityContext()
                .identity().permissions().contains("admin:system"));
        }
    }
}
