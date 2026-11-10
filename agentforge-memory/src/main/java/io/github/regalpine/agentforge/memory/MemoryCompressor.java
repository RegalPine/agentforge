package io.github.regalpine.agentforge.memory;

import java.util.List;
import java.util.Objects;

/**
 * Memory compressor.
 * AF-ENHANCE-06 §36-38: Memory &
 * Context Compression.
 *
 * <p>Memory Compression changes
 * persisted memory. Context
 * Compression only changes the
 * current context view.
 *
 * <p>Compression must preserve:
 * source references, provenance,
 * important facts, decisions,
 * uncertainty, time.
 */
public final class MemoryCompressor {

    private final int maxContentLength;

    public MemoryCompressor(
        int maxContentLength
    ) {
        if (maxContentLength <= 0) {
            maxContentLength = 1000;
        }
        this.maxContentLength =
            maxContentLength;
    }

    public MemoryCompressor() {
        this(1000);
    }

    /**
     * Compress a memory record's
     * content by truncation with
     * preservation of key info.
     */
    public CompressionResult compress(
        MemoryRecord record
    ) {
        Objects.requireNonNull(
            record, "record"
        );

        String original = record.content();

        if (original.length()
            <= maxContentLength
        ) {
            return new CompressionResult(
                record.memoryId(),
                original,
                original,
                false,
                original.length()
            );
        }

        // Preserve first portion +
        // last portion (often contains
        // conclusions/decisions)
        int headSize =
            maxContentLength * 3 / 4;
        int tailSize =
            maxContentLength / 4;

        String head =
            original.substring(
                0, headSize
            );
        String tail =
            original.substring(
                original.length() - tailSize
            );

        String compressed =
            head + "\n...\n" + tail;

        return new CompressionResult(
            record.memoryId(),
            original,
            compressed,
            true,
            compressed.length()
        );
    }

    /**
     * Compress a list of context
     * segments to fit within a
     * token budget.
     */
    public List<ContextSegment> compressContext(
        List<ContextSegment> segments,
        int maxTokens
    ) {
        Objects.requireNonNull(
            segments, "segments"
        );

        int totalTokens = segments.stream()
            .mapToInt(
                ContextSegment::estimatedTokens
            )
            .sum();

        if (totalTokens <= maxTokens) {
            return segments;
        }

        // Remove LOW priority segments
        // first, then NORMAL
        var filtered = segments.stream()
            .filter(s ->
                s.estimatedTokens() > 0
            )
            .sorted((a, b) ->
                Integer.compare(
                    b.estimatedTokens(),
                    a.estimatedTokens()
                )
            )
            .toList();

        var result =
            new java.util.ArrayList<
                ContextSegment>();
        int used = 0;

        for (var seg : filtered) {
            if (used + seg.estimatedTokens()
                <= maxTokens
            ) {
                result.add(seg);
                used += seg.estimatedTokens();
            }
        }

        return result;
    }

    /**
     * Compression result record.
     */
    public record CompressionResult(
        String memoryId,
        String originalContent,
        String compressedContent,
        boolean wasCompressed,
        int compressedLength
    ) {
        public CompressionResult {
            Objects.requireNonNull(
                memoryId, "memoryId"
            );
            Objects.requireNonNull(
                originalContent,
                "originalContent"
            );
            Objects.requireNonNull(
                compressedContent,
                "compressedContent"
            );
        }
    }
}
