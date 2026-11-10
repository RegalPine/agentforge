package io.github.regalpine.agentforge.model;

/**
 * Classification of model provider types.
 */
public enum ProviderType {

    /**
     * Remote provider accessing cloud model services (OpenAI, Anthropic, Azure).
     */
    REMOTE,

    /**
     * Provider compatible with OpenAI API protocol (vLLM, LM Studio, enterprise gateway).
     */
    OPENAI_COMPATIBLE,

    /**
     * Local provider running models locally (Ollama, local runtime).
     */
    LOCAL,

    /**
     * Enterprise model gateway provider.
     */
    GATEWAY
}
