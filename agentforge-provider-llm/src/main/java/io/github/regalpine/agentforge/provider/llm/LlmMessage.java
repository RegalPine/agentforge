package io.github.regalpine.agentforge.provider.llm;

public record LlmMessage(
    String role,
    String content
) {
}
