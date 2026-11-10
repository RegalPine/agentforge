package io.github.regalpine.agentforge.provider.http;

import io.github.regalpine.agentforge.capability.*;
import io.github.regalpine.agentforge.kernel.EntityId;
import io.github.regalpine.agentforge.kernel.Version;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class HttpProviderTest {

    @Test
    void shouldBlockLoopback() {
        var validator = new SsrfValidator();

        assertThrows(
            SecurityException.class,
            () -> validator.validate(
                URI.create(
                    "http://127.0.0.1/api"
                )
            )
        );
    }

    @Test
    void shouldBlockLocalhost() {
        var validator = new SsrfValidator();

        assertThrows(
            SecurityException.class,
            () -> validator.validate(
                URI.create(
                    "http://localhost/api"
                )
            )
        );
    }

    @Test
    void shouldBlockMetadataEndpoint() {
        var validator = new SsrfValidator();

        assertThrows(
            SecurityException.class,
            () -> validator.validate(
                URI.create(
                    "http://169.254.169.254/metadata"
                )
            )
        );
    }

    @Test
    void shouldBlockFileScheme() {
        var validator = new SsrfValidator();

        assertThrows(
            SecurityException.class,
            () -> validator.validate(
                URI.create(
                    "file:///etc/passwd"
                )
            )
        );
    }

    @Test
    void shouldReturnFailureForMissingUrl() {
        HttpProvider provider = new HttpProvider();

        CapabilityInvocation invocation =
            new CapabilityInvocation(
                "inv-001",
                new EntityId("http.request"),
                new Version("1.0.0"),
                Map.of(),
                null
            );

        var result = provider.invoke(invocation);

        assertInstanceOf(
            CapabilityFailure.class,
            result
        );
    }

    @Test
    void shouldDeclareCorrectDescriptor() {
        HttpProvider provider = new HttpProvider();

        var desc = provider.descriptor();

        assertEquals(
            "http",
            desc.providerId()
        );
        assertEquals(1, desc.capabilities().size());
    }

    @Test
    void shouldBlockSsrfAndReturnFailure() {
        HttpProvider provider = new HttpProvider();

        CapabilityInvocation invocation =
            new CapabilityInvocation(
                "inv-002",
                new EntityId("http.request"),
                new Version("1.0.0"),
                Map.of(
                    "method", "GET",
                    "url",
                    "http://127.0.0.1/secret"
                ),
                null
            );

        var result = provider.invoke(invocation);

        assertInstanceOf(
            CapabilityFailure.class,
            result
        );

        assertEquals(
            "AF-HTTP-003",
            ((CapabilityFailure) result).code()
        );
    }
}
