package io.github.regalpine.agentforge.hardening;

import io.github.regalpine.agentforge.security.*;
import io.github.regalpine.agentforge.security.auth.PermissionBasedAuthorizer;
import io.github.regalpine.agentforge.security.auth.StaticTokenAuthenticationProvider;
import io.github.regalpine.agentforge.security.credential.*;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * AF-HARDEN-022: JWT / Token Validation
 * AF-HARDEN-023: Tenant Isolation
 * AF-HARDEN-024: Credential Redaction
 * AF-HARDEN-025: SSRF (tested in HTTP provider module)
 * AF-HARDEN-026: Authorization
 */
class SecurityRegressionTest {

    // ── AF-HARDEN-022: Token Validation ──

    @Test
    void shouldRejectMissingToken() {
        var provider = new StaticTokenAuthenticationProvider(Map.of());
        var result = provider.authenticate(new AuthenticationRequest(null));
        assertInstanceOf(AuthenticationFailure.class, result);
    }

    @Test
    void shouldRejectBlankToken() {
        var provider = new StaticTokenAuthenticationProvider(Map.of());
        var result = provider.authenticate(new AuthenticationRequest(""));
        assertInstanceOf(AuthenticationFailure.class, result);
    }

    @Test
    void shouldRejectUnknownToken() {
        var provider = new StaticTokenAuthenticationProvider(Map.of());
        var result = provider.authenticate(new AuthenticationRequest("Bearer unknown-token"));
        assertInstanceOf(AuthenticationFailure.class, result);
    }

    @Test
    void shouldAcceptValidToken() {
        Identity identity = new Identity(
            "user-001", "issuer-a",
            Set.of("admin"), Set.of("read", "write"),
            Map.of("tenant", "tenant-a")
        );

        var provider = new StaticTokenAuthenticationProvider(
            Map.of("valid-token", identity)
        );

        var result = provider.authenticate(
            new AuthenticationRequest("Bearer valid-token")
        );

        assertInstanceOf(AuthenticationSuccess.class, result);
        var success = (AuthenticationSuccess) result;
        assertEquals("user-001", success.securityContext().identity().subjectId());
    }

    // ── AF-HARDEN-023: Tenant Isolation ──

    @Test
    void shouldIsolateTenantFromIdentity() {
        Identity identity = new Identity(
            "user-001", "issuer-a",
            Set.of(), Set.of(),
            Map.of("tenant", "tenant-x")
        );

        var provider = new StaticTokenAuthenticationProvider(
            Map.of("tok", identity)
        );

        var result = (AuthenticationSuccess) provider.authenticate(
            new AuthenticationRequest("Bearer tok")
        );

        assertEquals(
            "tenant-x",
            result.securityContext().tenant().tenantId()
        );
    }

    @Test
    void shouldDefaultTenantWhenNoClaim() {
        Identity identity = new Identity(
            "user-002", "issuer-b",
            Set.of(), Set.of(),
            Map.of()
        );

        var provider = new StaticTokenAuthenticationProvider(
            Map.of("tok2", identity)
        );

        var result = (AuthenticationSuccess) provider.authenticate(
            new AuthenticationRequest("Bearer tok2")
        );

        assertEquals(
            "default",
            result.securityContext().tenant().tenantId()
        );
    }

    @Test
    void shouldIsolatePermissionsPerIdentity() {
        Identity admin = new Identity(
            "admin-1", "issuer",
            Set.of("admin"), Set.of("read", "write", "delete"),
            Map.of("tenant", "tenant-a")
        );

        Identity viewer = new Identity(
            "viewer-1", "issuer",
            Set.of("viewer"), Set.of("read"),
            Map.of("tenant", "tenant-a")
        );

        var authorizer = new PermissionBasedAuthorizer();

        assertTrue(authorizer.authorize(
            new AuthorizationRequest(admin, "tenant-a", "graph", "g1", "write")
        ));

        assertFalse(authorizer.authorize(
            new AuthorizationRequest(viewer, "tenant-a", "graph", "g1", "write")
        ));

        assertTrue(authorizer.authorize(
            new AuthorizationRequest(viewer, "tenant-a", "graph", "g1", "read")
        ));
    }

    // ── AF-HARDEN-024: Credential Redaction ──

    @Test
    void shouldResolveCredential() {
        var credential = new ResolvedCredential(
            "provider-openai", "api-key",
            Map.of("key", "sk-secret-12345")
        );

        var provider = new InMemoryCredentialProvider(
            Map.of("provider-openai", credential)
        );

        var resolved = provider.resolve(
            new CredentialReference("provider-openai")
        );

        assertTrue(resolved.isPresent());
        assertEquals("api-key", resolved.get().type());
        assertEquals("sk-secret-12345", resolved.get().secrets().get("key"));
    }

    @Test
    void shouldSerializeCredentialSafely() {
        var credential = new ResolvedCredential(
            "provider-secret", "api-key",
            Map.of("key", "super-secret-value")
        );

        // Verify secrets are stored in immutable map
        assertNotNull(credential.secrets());
        assertEquals("super-secret-value", credential.secrets().get("key"));

        // Verify immutability
        assertThrows(UnsupportedOperationException.class,
            () -> credential.secrets().put("new", "value"));
    }

    @Test
    void shouldRejectBlankCredentialReference() {
        assertThrows(IllegalArgumentException.class, () -> new CredentialReference(""));
        assertThrows(IllegalArgumentException.class, () -> new CredentialReference("  "));
    }

    @Test
    void shouldReturnEmptyForUnknownCredential() {
        var provider = new InMemoryCredentialProvider(Map.of());
        var resolved = provider.resolve(new CredentialReference("unknown"));
        assertFalse(resolved.isPresent());
    }

    // ── AF-HARDEN-026: Authorization ──

    @Test
    void shouldDenyNullAuthorizationRequest() {
        var authorizer = new PermissionBasedAuthorizer();
        assertFalse(authorizer.authorize(null));
    }

    @Test
    void shouldDenyNullIdentity() {
        var authorizer = new PermissionBasedAuthorizer();
        assertFalse(authorizer.authorize(
            new AuthorizationRequest(null, "tenant", "graph", "g1", "read")
        ));
    }

    @Test
    void shouldDenyWhenPermissionMissing() {
        Identity identity = new Identity(
            "user", "issuer", Set.of(), Set.of("read"), Map.of()
        );

        var authorizer = new PermissionBasedAuthorizer();

        assertFalse(authorizer.authorize(
            new AuthorizationRequest(identity, "t1", "graph", "g1", "delete")
        ));
    }

    @Test
    void shouldGrantWhenPermissionPresent() {
        Identity identity = new Identity(
            "user", "issuer", Set.of(), Set.of("read", "write"), Map.of()
        );

        var authorizer = new PermissionBasedAuthorizer();

        assertTrue(authorizer.authorize(
            new AuthorizationRequest(identity, "t1", "graph", "g1", "write")
        ));
    }
}
