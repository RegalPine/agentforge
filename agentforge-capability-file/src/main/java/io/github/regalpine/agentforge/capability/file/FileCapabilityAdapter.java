package io.github.regalpine.agentforge.capability.file;

import io.github.regalpine.agentforge.capability.*;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.time.Instant;
import java.util.*;
import java.util.stream.Stream;

/**
 * File capability adapter.
 * AF-ENHANCE-02 §15-16: performs
 * file operations within a sandboxed
 * base path.
 *
 * <p>Security model:
 * <ul>
 *   <li>All paths are resolved and
 *       validated against basePath</li>
 *   <li>Path traversal attacks
 *       (../) are blocked</li>
 *   <li>Extension whitelist enforced</li>
 *   <li>File size limits enforced</li>
 * </ul>
 */
public final class FileCapabilityAdapter
    implements CapabilityAdapter {

    private final FileCapabilityDescriptor descriptor;

    private volatile AdapterHealthStatus healthStatus =
        AdapterHealthStatus.UNKNOWN;

    public FileCapabilityAdapter(
        FileCapabilityDescriptor descriptor
    ) {
        Objects.requireNonNull(
            descriptor, "descriptor"
        );

        this.descriptor = descriptor;

        // Verify basePath exists
        if (!Files.isDirectory(
            descriptor.basePath()
        )) {
            throw new IllegalArgumentException(
                "basePath does not exist or is "
                    + "not a directory: "
                    + descriptor.basePath()
            );
        }

        this.healthStatus =
            AdapterHealthStatus.AVAILABLE;
    }

    @Override
    public AdapterMetadata metadata() {
        return new AdapterMetadata(
            "file-adapter-"
                + descriptor.capabilityId(),
            "file",
            "1.0.0",
            "File Adapter: "
                + descriptor.name(),
            descriptor.description(),
            Set.of(
                SideEffectClassification.READ_ONLY,
                SideEffectClassification.WRITE
            ),
            Map.of(),
            descriptor.metadata()
        );
    }

    @Override
    public CapabilityResult execute(
        CapabilityInvocation invocation
    ) {
        try {
            return switch (descriptor.operation()) {
                case READ ->
                    readFile(invocation);
                case WRITE ->
                    writeFile(invocation);
                case LIST ->
                    listFiles(invocation);
                case DELETE ->
                    deleteFile(invocation);
            };

        } catch (FileSandboxViolationException e) {
            return new CapabilityFailure(
                "SANDBOX_VIOLATION",
                e.getMessage(),
                false
            );

        } catch (Exception e) {
            return new CapabilityFailure(
                "FILE_ERROR",
                e.getMessage(),
                true
            );
        }
    }

    @Override
    public AdapterHealth health() {
        boolean dirExists =
            Files.isDirectory(
                descriptor.basePath()
            );

        healthStatus = dirExists
            ? AdapterHealthStatus.AVAILABLE
            : AdapterHealthStatus.UNAVAILABLE;

        return new AdapterHealth(
            healthStatus,
            Instant.now(),
            null,
            Map.of(
                "basePath",
                descriptor.basePath().toString(),
                "operation",
                descriptor.operation().name()
            )
        );
    }

    private CapabilityResult readFile(
        CapabilityInvocation invocation
    ) throws IOException {
        String relativePath =
            getRequiredPath(invocation);

        Path resolved =
            resolveAndValidate(relativePath);

        // Check file size
        long fileSize =
            Files.size(resolved);

        if (fileSize > descriptor.maxFileSize()) {
            throw new FileSandboxViolationException(
                "File exceeds max size: "
                    + fileSize + " > "
                    + descriptor.maxFileSize()
            );
        }

        String content = Files.readString(
            resolved,
            StandardCharsets.UTF_8
        );

        Map<String, Object> output =
            new LinkedHashMap<>();

        output.put("path", relativePath);
        output.put("content", content);
        output.put("size", fileSize);

        return new CapabilitySuccess(output);
    }

    private CapabilityResult writeFile(
        CapabilityInvocation invocation
    ) throws IOException {
        String relativePath =
            getRequiredPath(invocation);

        Path resolved =
            resolveAndValidate(relativePath);

        Object contentObj =
            invocation.arguments().get("content");

        if (contentObj == null) {
            return new CapabilityFailure(
                "INVALID_ARGUMENT",
                "'content' argument required",
                false
            );
        }

        String content = contentObj.toString();

        // Check content size
        byte[] bytes = content.getBytes(
            StandardCharsets.UTF_8
        );

        if (bytes.length > descriptor.maxFileSize()) {
            throw new FileSandboxViolationException(
                "Content exceeds max size: "
                    + bytes.length + " > "
                    + descriptor.maxFileSize()
            );
        }

        // Ensure parent dirs exist
        Path parent = resolved.getParent();

        if (parent != null) {
            Files.createDirectories(parent);
        }

        Files.write(
            resolved,
            bytes,
            StandardOpenOption.CREATE,
            StandardOpenOption.TRUNCATE_EXISTING
        );

        Map<String, Object> output =
            new LinkedHashMap<>();

        output.put("path", relativePath);
        output.put("bytesWritten", bytes.length);

        return new CapabilitySuccess(output);
    }

    private CapabilityResult listFiles(
        CapabilityInvocation invocation
    ) throws IOException {
        String relativePath =
            invocation.arguments() != null
                ? (String) invocation.arguments()
                    .getOrDefault("path", ".")
                : ".";

        Path resolved =
            resolveAndValidate(relativePath);

        if (!Files.isDirectory(resolved)) {
            return new CapabilityFailure(
                "NOT_A_DIRECTORY",
                "Path is not a directory: "
                    + relativePath,
                false
            );
        }

        List<String> entries = new ArrayList<>();

        try (Stream<Path> stream =
                 Files.list(resolved)
        ) {
            stream.forEach(p -> {
                String name =
                    p.getFileName().toString();

                entries.add(
                    Files.isDirectory(p)
                        ? name + "/"
                        : name
                );
            });
        }

        Collections.sort(entries);

        Map<String, Object> output =
            new LinkedHashMap<>();

        output.put("path", relativePath);
        output.put("entries", entries);
        output.put("count", entries.size());

        return new CapabilitySuccess(output);
    }

    private CapabilityResult deleteFile(
        CapabilityInvocation invocation
    ) throws IOException {
        String relativePath =
            getRequiredPath(invocation);

        Path resolved =
            resolveAndValidate(relativePath);

        boolean deleted =
            Files.deleteIfExists(resolved);

        Map<String, Object> output =
            new LinkedHashMap<>();

        output.put("path", relativePath);
        output.put("deleted", deleted);

        return new CapabilitySuccess(output);
    }

    private String getRequiredPath(
        CapabilityInvocation invocation
    ) {
        Object pathObj =
            invocation.arguments().get("path");

        if (pathObj == null) {
            throw new FileSandboxViolationException(
                "'path' argument required"
            );
        }

        return pathObj.toString();
    }

    /**
     * Resolve a relative path against
     * basePath and validate sandbox
     * constraints.
     */
    private Path resolveAndValidate(
        String relativePath
    ) {
        Path resolved = descriptor.basePath()
            .resolve(relativePath)
            .normalize();

        // Sandbox check: resolved path must
        // start with basePath
        if (!resolved.startsWith(
            descriptor.basePath()
        )) {
            throw new FileSandboxViolationException(
                "Path traversal blocked: '"
                    + relativePath
                    + "' resolves outside "
                    + "basePath"
            );
        }

        // Extension whitelist check
        Set<String> allowed =
            descriptor.allowedExtensions();

        if (!allowed.isEmpty()
            && !Files.isDirectory(resolved)
        ) {
            String fileName =
                resolved.getFileName().toString();

            int dotIndex =
                fileName.lastIndexOf('.');

            String ext = dotIndex >= 0
                ? fileName.substring(dotIndex + 1)
                : "";

            if (!allowed.contains(ext)) {
                throw new FileSandboxViolationException(
                    "Extension not allowed: ."
                        + ext + ". Allowed: "
                        + allowed
                );
            }
        }

        return resolved;
    }

    /**
     * Thrown when a file operation
     * violates sandbox constraints.
     */
    public static class FileSandboxViolationException
        extends RuntimeException {

        public FileSandboxViolationException(
            String message
        ) {
            super(message);
        }
    }
}
