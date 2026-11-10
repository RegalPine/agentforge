package io.github.regalpine.agentforge.rag.ingestion;

import io.github.regalpine.agentforge.rag.model.ChunkLocation;
import io.github.regalpine.agentforge.rag.model.DocumentClassification;
import io.github.regalpine.agentforge.rag.model.KnowledgeChunk;

import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Structural chunker — structure-first chunking with token fallback.
 * AE-09 §17-20: Chunk, Chunking Principles, Chunk Overlap.
 *
 * <p>Chunks a parsed document by respecting document structure
 * (headings, sections, paragraphs). Falls back to token-length
 * based splitting when a section exceeds the maximum size.
 */
public final class StructuralChunker {

    private final int maxChunkTokens;
    private final int overlapTokens;

    public StructuralChunker(int maxChunkTokens, int overlapTokens) {
        if (maxChunkTokens <= 0) maxChunkTokens = 512;
        if (overlapTokens < 0) overlapTokens = 50;
        this.maxChunkTokens = maxChunkTokens;
        this.overlapTokens = overlapTokens;
    }

    public StructuralChunker() {
        this(512, 50);
    }

    /**
     * Chunk a parsed document into knowledge chunks.
     */
    public List<KnowledgeChunk> chunk(
            ParsedDocument parsed,
            String sourceId,
            String tenantId,
            DocumentClassification classification) {

        List<KnowledgeChunk> chunks = new ArrayList<>();
        String currentSection = "";
        int paragraphIndex = 0;

        if (parsed.hasStructure()) {
            StringBuilder sectionBuffer = new StringBuilder();
            String sectionHeading = "";

            for (DocumentElement element : parsed.elements()) {
                if (element.type() == DocumentElementType.HEADING) {
                    if (!sectionBuffer.isEmpty()) {
                        splitAndAdd(sectionBuffer.toString(), sectionHeading,
                                parsed.documentId(), sourceId, tenantId,
                                classification, paragraphIndex, chunks);
                        paragraphIndex++;
                    }
                    sectionHeading = element.content();
                    currentSection = sectionHeading;
                    sectionBuffer.setLength(0);
                } else {
                    if (!sectionBuffer.isEmpty()) {
                        sectionBuffer.append("\n");
                    }
                    sectionBuffer.append(element.content());
                }
            }

            if (!sectionBuffer.isEmpty()) {
                splitAndAdd(sectionBuffer.toString(), sectionHeading,
                        parsed.documentId(), sourceId, tenantId,
                        classification, paragraphIndex, chunks);
            }
        } else {
            splitAndAdd(parsed.text(), "", parsed.documentId(), sourceId, tenantId,
                    classification, 0, chunks);
        }

        return chunks;
    }

    private void splitAndAdd(
            String text,
            String section,
            String documentId,
            String sourceId,
            String tenantId,
            DocumentClassification classification,
            int paragraphIndex,
            List<KnowledgeChunk> chunks) {

        int textTokens = text.length() / 4;

        if (textTokens <= maxChunkTokens) {
            chunks.add(createChunk(documentId, sourceId, tenantId, text, section,
                    paragraphIndex, classification, chunks.size()));
        } else {
            List<String> subChunks = splitByTokenLimit(text, maxChunkTokens, overlapTokens);
            for (int i = 0; i < subChunks.size(); i++) {
                chunks.add(createChunk(documentId, sourceId, tenantId, subChunks.get(i),
                        section, paragraphIndex + i, classification, chunks.size()));
            }
        }
    }

    private KnowledgeChunk createChunk(
            String documentId, String sourceId, String tenantId,
            String text, String section, int paragraph,
            DocumentClassification classification, int index) {

        String chunkId = documentId + "-chunk-" + index;
        ChunkLocation location = ChunkLocation.of(0, section, paragraph);
        String checksum = computeChecksum(text);

        return new KnowledgeChunk(
                chunkId, documentId, sourceId, tenantId,
                text, location, checksum, classification, "en",
                null, Map.of()
        );
    }

    private List<String> splitByTokenLimit(String text, int maxTokens, int overlapTokens) {
        List<String> result = new ArrayList<>();
        int charPerToken = 4;
        int maxChars = maxTokens * charPerToken;
        int overlapChars = overlapTokens * charPerToken;

        int start = 0;
        while (start < text.length()) {
            int end = Math.min(start + maxChars, text.length());

            if (end < text.length()) {
                int lastNewline = text.lastIndexOf('\n', end);
                if (lastNewline > start + maxChars / 2) {
                    end = lastNewline;
                }
            }

            result.add(text.substring(start, end).trim());
            start = end - overlapChars;
            if (start < 0) start = 0;
            if (end >= text.length()) break;
        }

        return result;
    }

    private String computeChecksum(String text) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(text.getBytes());
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < 8; i++) {
                sb.append(String.format("%02x", hash[i]));
            }
            return sb.toString();
        } catch (Exception e) {
            return Integer.toHexString(text.hashCode());
        }
    }
}
