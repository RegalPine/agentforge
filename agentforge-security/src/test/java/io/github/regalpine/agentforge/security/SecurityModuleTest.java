package io.github.regalpine.agentforge.security;

import io.github.regalpine.agentforge.security.auth.PermissionBasedAuthorizer;
import io.github.regalpine.agentforge.security.auth.StaticTokenAuthenticationProvider;
import io.github.regalpine.agentforge.security.credential.*;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class SecurityModuleTest {

    private final Identity testIdentity =
        new Identity(
            "user-001",
            "agentforge",
            Set.of("admin"),
            Set.of(
                "graph:read",
                "graph:create",
                "execution:create",
                "execution:read"
            ),
            Map.of("tenant", "tenant-a")
        );

    @Test
    void shouldAuthenticateValidToken() {
        var provider =
            new StaticTokenAuthenticationProvider(
                Map.of("token-123", testIdentity)
            );

        AuthenticationResult result =
            provider.authenticate(
                new AuthenticationRequest(
                    "Bearer token-123"
                )
            );

        assertInstanceOf(
            AuthenticationSuccess.class,
            result
        );

        var success =
            (AuthenticationSuccess) result;

        assertEquals(
            "user-001",
            success.securityContext()
                .identity().subjectId()
        );

        assertEquals(
            "tenant-a",
            success.securityContext()
                .tenant().tenantId()
        );
    }

    @Test
    void shouldRejectInvalidToken() {
        var provider =
            new StaticTokenAuthenticationProvider(
                Map.of("token-123", testIdentity)
            );

        AuthenticationResult result =
            provider.authenticate(
                new AuthenticationRequest(
                    "Bearer wrong-token"
                )
            );

        assertInstanceOf(
            AuthenticationFailure.class,
            result
        );

        var failure =
            (AuthenticationFailure) result;

        assertEquals(
            "AF-SEC-002",
            failure.code()
        );
    }

    @Test
    void shouldRejectMissingHeader() {
        var provider =
            new StaticTokenAuthenticationProvider(
                Map.of()
            );

        AuthenticationResult result =
            provider.authenticate(
                new AuthenticationRequest(null)
            );

        assertInstanceOf(
            AuthenticationFailure.class,
            result
        );
    }

    @Test
    void shouldAuthorizeWithPermission() {
        var authorizer =
            new PermissionBasedAuthorizer();

        boolean result = authorizer.authorize(
            new AuthorizationRequest(
                testIdentity,
                "tenant-a",
                "graph",
                "graph-001",
                "graph:read"
            )
        );

        assertTrue(result);
    }

    @Test
    void shouldDenyWithoutPermission() {
        var authorizer =
            new PermissionBasedAuthorizer();

        boolean result = authorizer.authorize(
            new AuthorizationRequest(
                testIdentity,
                "tenant-a",
                "graph",
                "graph-001",
                "graph:delete"
            )
        );

        assertFalse(result);
    }

    @Test
    void shouldDenyNullRequest() {
        var authorizer =
            new PermissionBasedAuthorizer();

        assertFalse(
            authorizer.authorize(null)
        );
    }

    @Test
    void shouldResolveCredential() {
        var credential =
            new ResolvedCredential(
                "provider-openai-prod",
                "api-key",
                Map.of("key", "sk-***")
            );

        var provider =
            new InMemoryCredentialProvider(
                Map.of(
                    "provider-openai-prod",
                    credential
                )
            );

        var resolved =
            provider.resolve(
                new CredentialReference(
                    "provider-openai-prod"
                )
            );

        assertTrue(resolved.isPresent());
        assertEquals(
            "api-key",
            resolved.get().type()
        );
    }

    @Test
    void shouldReturnEmptyForUnknownCredential() {
        var provider =
            new InMemoryCredentialProvider(
                Map.of()
            );

        var resolved =
            provider.resolve(
                new CredentialReference(
                    "unknown-ref"
                )
            );

        assertFalse(resolved.isPresent());
    }

    @Test
    void shouldRejectBlankCredentialRef() {
        assertThrows(
            IllegalArgumentException.class,
            () -> new CredentialReference("")
        );
    }

    @Test
    void shouldIsolateTenantFromIdentity() {
        var provider =
            new StaticTokenAuthenticationProvider(
                Map.of("tok", testIdentity)
            );

        var result =
            (AuthenticationSuccess) provider.authenticate(
                new AuthenticationRequest("Bearer tok")
            );

        assertEquals(
            "tenant-a",
            result.securityContext()
                .tenant().tenantId()
        );

        assertEquals(
            "user-001",
            result.securityContext()
                .tenant().subjectId()
        );
    }
}
