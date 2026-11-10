package io.github.regalpine.agentforge.capability.file;

import io.github.regalpine.agentforge.capability.SideEffectClassification;

import java.nio.file.Path;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * File capability descriptor.
 * AF-ENHANCE-02 §15: describes a
 * file operation that can be invoked
 * as a capability.
 *
 * <p>Sandbox constraints:
 * <ul>
 *   <li>basePath — all operations
 *       confined within this directory</li>
 *   <li>allowedExtensions — file
 *       extension whitelist</li>
 *   <li>maxFileSize — maximum file
 *       size in bytes</li>
 * </ul>
 */
public record FileCapabilityDescriptor(
    String capabilityId,
    String name,
    String description,
    Path basePath,
    FileOperation operation,
    Set<String> allowedExtensions,
    long maxFileSize,
    SideEffectClassification sideEffect,
    Map<String, Object> metadata
) {

    public FileCapabilityDescriptor {
        Objects.requireNonNull(
            capabilityId, "capabilityId"
        );

        Objects.requireNonNull(
            name, "name"
        );

        Objects.requireNonNull(
            basePath, "basePath"
        );

        Objects.requireNonNull(
            operation, "operation"
        );

        if (capabilityId.isBlank()) {
            throw new IllegalArgumentException(
                "capabilityId cannot be blank"
            );
        }

        basePath = basePath.toAbsolutePath()
            .normalize();

        allowedExtensions =
            allowedExtensions == null
                ? Set.of()
                : Set.copyOf(allowedExtensions);

        if (maxFileSize <= 0) {
            maxFileSize = 10 * 1024 * 1024;
        }

        sideEffect =
            sideEffect == null
                ? inferSideEffect(operation)
                : sideEffect;

        metadata =
            metadata == null
                ? Map.of()
                : Map.copyOf(metadata);
    }

    private static SideEffectClassification
    inferSideEffect(
        FileOperation op
    ) {
        return switch (op) {
            case READ, LIST ->
                SideEffectClassification.READ_ONLY;
            case WRITE, DELETE ->
                SideEffectClassification.WRITE;
        };
    }
}
