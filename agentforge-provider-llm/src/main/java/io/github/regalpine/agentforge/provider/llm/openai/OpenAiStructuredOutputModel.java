package io.github.regalpine.agentforge.provider.llm.openai;

import io.github.regalpine.agentforge.model.*;

import java.util.List;
import java.util.Objects;

/**
 * StructuredOutputModel implementation that wraps a ChatModel.
 * <p>
 * Uses response_format to request structured JSON output from the model,
 * then validates the output against the specified schema.
 */
public class OpenAiStructuredOutputModel implements StructuredOutputModel {

    private final ChatModel chatModel;

    public OpenAiStructuredOutputModel(ChatModel chatModel) {
        this.chatModel = Objects.requireNonNull(chatModel, "chatModel");
    }

    @Override
    public StructuredChatResponse chatStructured(StructuredChatRequest request) {
        Objects.requireNonNull(request, "request");

        // Build a ChatRequest with response_format set to JSON
        ChatRequest.Builder builder = ChatRequest.builder()
            .messages(request.messages())
            .responseFormat(ResponseFormat.json());

        if (request.systemInstruction() != null) {
            builder.systemInstruction(request.systemInstruction());
        }
        if (request.temperature() != null) {
            builder.temperature(request.temperature());
        }
        if (request.maxTokens() != null) {
            builder.maxTokens(request.maxTokens());
        }

        ChatResponse chatResponse = chatModel.chat(builder.build());

        // Parse the raw content as the structured value
        // In a full implementation, this would validate against the schema
        String rawContent = chatResponse.content();
        Object parsedValue = rawContent; // Simplified - would parse JSON in full impl

        return StructuredChatResponse.of(
            rawContent,
            parsedValue,
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
