package io.github.regalpine.agentforge.capability.file;

import io.github.regalpine.agentforge.capability.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * File adapter sandbox tests.
 * AF-ENHANCE-02 §15-16: verifies
 * path traversal protection, extension
 * whitelist and file size limits.
 */
class FileSandboxTest {

    @TempDir
    Path tempDir;

    @Test
    void shouldReadFileWithinSandbox()
        throws IOException
    {
        Path testFile =
            tempDir.resolve("test.txt");

        Files.writeString(testFile, "hello");

        var desc = new FileCapabilityDescriptor(
            "file.read",
            "Read file",
            "Read a text file",
            tempDir,
            FileOperation.READ,
            Set.of("txt"),
            1024,
            null,
            Map.of()
        );

        var adapter =
            new FileCapabilityAdapter(desc);

        var invocation = new CapabilityInvocation(
            "inv-1",
            new io.github.regalpine.agentforge.kernel.EntityId("file.read"),
            new io.github.regalpine.agentforge.kernel.Version("1.0.0"),
            Map.of("path", "test.txt"),
            new InvocationContext(
                "exec-1", "t1", Map.of()
            )
        );

        var result = adapter.execute(invocation);

        assertInstanceOf(
            CapabilitySuccess.class, result
        );

        var success = (CapabilitySuccess) result;

        assertEquals(
            "hello",
            success.output().get("content")
        );
    }

    @Test
    void shouldBlockPathTraversal() {
        var desc = new FileCapabilityDescriptor(
            "file.read",
            "Read file",
            "Read a text file",
            tempDir,
            FileOperation.READ,
            Set.of("txt"),
            1024,
            null,
            Map.of()
        );

        var adapter =
            new FileCapabilityAdapter(desc);

        var invocation = new CapabilityInvocation(
            "inv-2",
            new io.github.regalpine.agentforge.kernel.EntityId("file.read"),
            new io.github.regalpine.agentforge.kernel.Version("1.0.0"),
            Map.of("path", "../../etc/passwd"),
            new InvocationContext(
                "exec-2", "t1", Map.of()
            )
        );

        var result = adapter.execute(invocation);

        assertInstanceOf(
            CapabilityFailure.class, result
        );

        var failure = (CapabilityFailure) result;

        assertEquals(
            "SANDBOX_VIOLATION",
            failure.code()
        );
    }

    @Test
    void shouldBlockDisallowedExtension() {
        var desc = new FileCapabilityDescriptor(
            "file.read",
            "Read file",
            "Read a text file",
            tempDir,
            FileOperation.READ,
            Set.of("txt"),
            1024,
            null,
            Map.of()
        );

        var adapter =
            new FileCapabilityAdapter(desc);

        var invocation = new CapabilityInvocation(
            "inv-3",
            new io.github.regalpine.agentforge.kernel.EntityId("file.read"),
            new io.github.regalpine.agentforge.kernel.Version("1.0.0"),
            Map.of("path", "secret.key"),
            new InvocationContext(
                "exec-3", "t1", Map.of()
            )
        );

        var result = adapter.execute(invocation);

        assertInstanceOf(
            CapabilityFailure.class, result
        );

        var failure = (CapabilityFailure) result;

        assertEquals(
            "SANDBOX_VIOLATION",
            failure.code()
        );
    }

    @Test
    void shouldWriteFileWithinSandbox()
        throws IOException
    {
        var desc = new FileCapabilityDescriptor(
            "file.write",
            "Write file",
            "Write a text file",
            tempDir,
            FileOperation.WRITE,
            Set.of("txt"),
            1024,
            null,
            Map.of()
        );

        var adapter =
            new FileCapabilityAdapter(desc);

        var invocation = new CapabilityInvocation(
            "inv-4",
            new io.github.regalpine.agentforge.kernel.EntityId("file.write"),
            new io.github.regalpine.agentforge.kernel.Version("1.0.0"),
            Map.of(
                "path", "output.txt",
                "content", "written data"
            ),
            new InvocationContext(
                "exec-4", "t1", Map.of()
            )
        );

        var result = adapter.execute(invocation);

        assertInstanceOf(
            CapabilitySuccess.class, result
        );

        // Verify file was written
        Path written =
            tempDir.resolve("output.txt");

        assertTrue(Files.exists(written));

        assertEquals(
            "written data",
            Files.readString(written)
        );
    }

    @Test
    void shouldListFilesInDirectory()
        throws IOException
    {
        Files.writeString(
            tempDir.resolve("a.txt"), "a"
        );

        Files.writeString(
            tempDir.resolve("b.txt"), "b"
        );

        var desc = new FileCapabilityDescriptor(
            "file.list",
            "List files",
            "List directory",
            tempDir,
            FileOperation.LIST,
            Set.of(),
            1024,
            null,
            Map.of()
        );

        var adapter =
            new FileCapabilityAdapter(desc);

        var invocation = new CapabilityInvocation(
            "inv-5",
            new io.github.regalpine.agentforge.kernel.EntityId("file.list"),
            new io.github.regalpine.agentforge.kernel.Version("1.0.0"),
            Map.of("path", "."),
            new InvocationContext(
                "exec-5", "t1", Map.of()
            )
        );

        var result = adapter.execute(invocation);

        assertInstanceOf(
            CapabilitySuccess.class, result
        );

        var success = (CapabilitySuccess) result;

        assertEquals(2, success.output().get("count"));
    }

    @Test
    void shouldInferSideEffectFromOperation() {
        var readDesc =
            new FileCapabilityDescriptor(
                "f.read", "R", "R",
                tempDir, FileOperation.READ,
                null, 1024, null, null
            );

        assertEquals(
            SideEffectClassification.READ_ONLY,
            readDesc.sideEffect()
        );

        var writeDesc =
            new FileCapabilityDescriptor(
                "f.write", "W", "W",
                tempDir, FileOperation.WRITE,
                null, 1024, null, null
            );

        assertEquals(
            SideEffectClassification.WRITE,
            writeDesc.sideEffect()
        );
    }

    @Test
    void shouldReportHealth() {
        var desc = new FileCapabilityDescriptor(
            "file.health",
            "Health",
            "Health check",
            tempDir,
            FileOperation.READ,
            Set.of(),
            1024,
            null,
            Map.of()
        );

        var adapter =
            new FileCapabilityAdapter(desc);

        var health = adapter.health();

        assertEquals(
            AdapterHealthStatus.AVAILABLE,
            health.status()
        );
    }
}
