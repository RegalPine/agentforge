package io.github.regalpine.agentforge.provider.openai;

import io.github.regalpine.agentforge.model.*;

import java.util.Objects;

/**
 * OpenAI StructuredOutputModel that delegates to a ChatModel
 * with JSON response format.
 */
class OpenAiProviderStructuredOutputModel implements StructuredOutputModel {

    private final ChatModel chatModel;

    OpenAiProviderStructuredOutputModel(ChatModel chatModel) {
        this.chatModel = Objects.requireNonNull(chatModel, "chatModel");
    }

    @Override
    public StructuredChatResponse chatStructured(StructuredChatRequest request) {
        Objects.requireNonNull(request, "request");

        ChatRequest chatRequest = ChatRequest.builder()
            .messages(request.messages())
            .systemInstruction(request.systemInstruction())
            .temperature(request.temperature())
            .maxTokens(request.maxTokens())
            .responseFormat(ResponseFormat.jsonSchema(request.outputSpec().schema()))
            .build();

        ChatResponse chatResponse = chatModel.chat(chatRequest);

        return StructuredChatResponse.of(
            chatResponse.content(),
            chatResponse.content(),
            request.outputSpec(),
            chatResponse.usage()
        );
    }

    @Override
    public String modelId() {
        return chatModel.modelId();
    }

    @Override
    public String providerId() {
        return chatModel.providerId();
    }
}
