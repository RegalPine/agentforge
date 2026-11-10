package io.github.regalpine.agentforge.rag.model;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Chunk location within a document.
 * AE-09 §18: Chunk Location.
 */
public final class ChunkLocation {

    private final int page;
    private final String section;
    private final int paragraph;
    private final int offset;
    private final int length;
    private final Map<String, Object> extra;

    public ChunkLocation(int page, String section, int paragraph, int offset, int length, Map<String, Object> extra) {
        this.page = page;
        this.section = section == null ? "" : section;
        this.paragraph = paragraph;
        this.offset = offset;
        this.length = length;
        this.extra = extra == null ? Map.of() : Map.copyOf(extra);
    }

    public ChunkLocation(int page, String section, int paragraph) {
        this(page, section, paragraph, 0, 0, null);
    }

    public static ChunkLocation of(int page, String section) {
        return new ChunkLocation(page, section, 0, 0, 0, null);
    }

    public static ChunkLocation of(int page, String section, int paragraph) {
        return new ChunkLocation(page, section, paragraph, 0, 0, null);
    }

    public int page() { return page; }
    public String section() { return section; }
    public int paragraph() { return paragraph; }
    public int offset() { return offset; }
    public int length() { return length; }
    public Map<String, Object> extra() { return extra; }

    public Optional<Object> extraValue(String key) {
        return Optional.ofNullable(extra.get(key));
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ChunkLocation that)) return false;
        return page == that.page && paragraph == that.paragraph
                && offset == that.offset && length == that.length
                && section.equals(that.section);
    }

    @Override
    public int hashCode() {
        return Objects.hash(page, section, paragraph, offset, length);
    }

    @Override
    public String toString() {
        return "ChunkLocation{page=" + page + ", section=" + section
                + ", para=" + paragraph + ", offset=" + offset + "}";
    }
}
