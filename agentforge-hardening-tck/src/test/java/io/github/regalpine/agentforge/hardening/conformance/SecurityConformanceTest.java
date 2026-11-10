package io.github.regalpine.agentforge.hardening.conformance;

import io.github.regalpine.agentforge.hardening.*;
import io.github.regalpine.agentforge.security.*;
import io.github.regalpine.agentforge.security.auth.*;
import io.github.regalpine.agentforge.security.credential.*;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * conformance-security: Identity
 * / AuthZ / Credential / Egress / Audit.
 * AF-RELEASE-02 §4, T-H6.15.
 */
class SecurityConformanceTest {

    @Test
    void afSec001_identityCreation() {
        var identity = new Identity(
            "user-1", "issuer",
            Set.of("admin"), Set.of("read"),
            Map.of("tenant", "t1")
        );
        assertEquals("user-1", identity.subjectId());
    }

    @Test
    void afSec002_authenticationSuccess() {
        var identity = new Identity(
            "user-2", "issuer",
            Set.of(), Set.of("read"),
            Map.of()
        );
        var provider = new StaticTokenAuthenticationProvider(
            Map.of("token-1", identity)
        );
        var result = provider.authenticate(
            new AuthenticationRequest("Bearer token-1")
        );
        assertInstanceOf(AuthenticationSuccess.class, result);
    }

    @Test
    void afSec003_authenticationFailure() {
        var provider = new StaticTokenAuthenticationProvider(Map.of());
        var result = provider.authenticate(
            new AuthenticationRequest("Bearer unknown")
        );
        assertInstanceOf(AuthenticationFailure.class, result);
    }

    @Test
    void afSec004_authorizationCheck() {
        var identity = new Identity(
            "user-3", "issuer",
            Set.of(), Set.of("read", "write"),
            Map.of()
        );
        var authorizer = new PermissionBasedAuthorizer();

        assertTrue(authorizer.authorize(
            new AuthorizationRequest(
                identity, "t1", "graph", "g1", "read"
            )
        ));
        assertFalse(authorizer.authorize(
            new AuthorizationRequest(
                identity, "t1", "graph", "g1", "delete"
            )
        ));
    }

    @Test
    void afSec005_credentialResolve() {
        var cred = new ResolvedCredential(
            "provider-1", "api-key",
            Map.of("key", "secret")
        );
        var provider = new InMemoryCredentialProvider(
            Map.of("provider-1", cred)
        );
        var resolved = provider.resolve(
            new CredentialReference("provider-1")
        );
        assertTrue(resolved.isPresent());
    }
}
