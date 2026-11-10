package io.github.regalpine.agentforge.security.iam;

import io.github.regalpine.agentforge.security.*;
import org.junit.jupiter.api.Test;
import java.time.*;
import java.util.Arrays;
import static org.junit.jupiter.api.Assertions.*;

class SharedSessionTest {
    private final InMemoryUserStore users = new InMemoryUserStore();
    private final InMemoryRoleStore roles = new InMemoryRoleStore();
    private final InMemoryTenantStore tenants = new InMemoryTenantStore();
    private final InMemorySessionStore sessions = new InMemorySessionStore();
    private final byte[] key = new byte[32];
    private final Instant start = Instant.parse("2026-01-01T00:00:00Z");

    SharedSessionTest() {
        Arrays.fill(key, (byte) 42);
        new IamBootstrap(tenants, users, roles).execute("test-password");
    }

    private SessionTokenProvider provider(Instant time) {
        return new SessionTokenProvider(users, roles, tenants, "default", sessions,
            key, Duration.ofMinutes(5), Clock.fixed(time, ZoneOffset.UTC));
    }

    @Test void separateProvidersShareSessionsAndRevocation() {
        var first = provider(start);
        String token = first.createSession("user-admin", "default");
        var second = provider(start.plusSeconds(1));
        assertInstanceOf(AuthenticationSuccess.class, second.validate(token));
        assertTrue(second.invalidate(token));
        assertInstanceOf(AuthenticationFailure.class, first.validate(token));
        assertFalse(second.invalidate(token));
    }

    @Test void expiryIsEnforcedAtTheBoundary() {
        String token = provider(start).createSession("user-admin", "default");
        assertInstanceOf(AuthenticationSuccess.class, provider(start.plusSeconds(299)).validate(token));
        assertInstanceOf(AuthenticationFailure.class, provider(start.plusSeconds(300)).validate(token));
        assertEquals(0, provider(start.plusSeconds(300)).activeSessionCount());
    }

    @Test void storesOnlyHashAndDefensivelyCopiesSigningKey() {
        var first = provider(start);
        Arrays.fill(key, (byte) 7);
        String token = first.createSession("user-admin", "default");
        assertTrue(sessions.find(token).isEmpty());
        assertInstanceOf(AuthenticationFailure.class, provider(start).validate(token));
        assertInstanceOf(AuthenticationSuccess.class, first.validate(token));
    }

    @Test void tenantSuspensionInvalidatesExistingSession() {
        var first = provider(start);
        String token = first.createSession("user-admin", "default");
        Tenant old = tenants.findById("default").orElseThrow();
        tenants.save(new Tenant(old.tenantId(), old.name(), old.displayName(), Tenant.TenantStatus.SUSPENDED,
            old.settings(), old.createdAt(), old.updatedAt()));
        assertInstanceOf(AuthenticationFailure.class, first.validate(token));
    }

    @Test void weakKeyAndNonPositiveLifetimeAreRejected() {
        assertThrows(IllegalArgumentException.class, () -> new SessionTokenProvider(users, roles, tenants,
            "default", sessions, new byte[8], Duration.ofMinutes(1), Clock.systemUTC()));
        assertThrows(IllegalArgumentException.class, () -> new SessionTokenProvider(users, roles, tenants,
            "default", sessions, key, Duration.ZERO, Clock.systemUTC()));
    }
}
