package io.github.regalpine.agentforge.model;

/**
 * Capabilities of a model.
 */
public record ModelCapabilities(
    boolean chat,
    boolean embedding,
    boolean rerank,
    boolean structuredOutput,
    boolean toolCalling,
    boolean streaming,
    boolean vision,
    Integer maxContextLength,
    Integer maxOutputLength
) {

    public ModelCapabilities {
    }

    /**
     * Create capabilities with all features enabled.
     *
     * @return a ModelCapabilities with all features enabled
     */
    public static ModelCapabilities all() {
        return new ModelCapabilities(true, true, true, true, true, true, true, null, null);
    }

    /**
     * Create capabilities for a chat-only model.
     *
     * @return a ModelCapabilities for chat-only
     */
    public static ModelCapabilities chatOnly() {
        return new ModelCapabilities(true, false, false, false, false, false, false, null, null);
    }

    /**
     * Create capabilities for an embedding-only model.
     *
     * @return a ModelCapabilities for embedding-only
     */
    public static ModelCapabilities embeddingOnly() {
        return new ModelCapabilities(false, true, false, false, false, false, false, null, null);
    }

    /**
     * Builder for ModelCapabilities.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for ModelCapabilities.
     */
    public static class Builder {
        private boolean chat = true;
        private boolean embedding = false;
        private boolean rerank = false;
        private boolean structuredOutput = false;
        private boolean toolCalling = false;
        private boolean streaming = false;
        private boolean vision = false;
        private Integer maxContextLength;
        private Integer maxOutputLength;

        public Builder chat(boolean chat) {
            this.chat = chat;
            return this;
        }

        public Builder embedding(boolean embedding) {
            this.embedding = embedding;
            return this;
        }

        public Builder rerank(boolean rerank) {
            this.rerank = rerank;
            return this;
        }

        public Builder structuredOutput(boolean structuredOutput) {
            this.structuredOutput = structuredOutput;
            return this;
        }

        public Builder toolCalling(boolean toolCalling) {
            this.toolCalling = toolCalling;
            return this;
        }

        public Builder streaming(boolean streaming) {
            this.streaming = streaming;
            return this;
        }

        public Builder vision(boolean vision) {
            this.vision = vision;
            return this;
        }

        public Builder maxContextLength(Integer maxContextLength) {
            this.maxContextLength = maxContextLength;
            return this;
        }

        public Builder maxOutputLength(Integer maxOutputLength) {
            this.maxOutputLength = maxOutputLength;
            return this;
        }

        public ModelCapabilities build() {
            return new ModelCapabilities(
                chat, embedding, rerank, structuredOutput,
                toolCalling, streaming, vision,
                maxContextLength, maxOutputLength
            );
        }
    }
}
