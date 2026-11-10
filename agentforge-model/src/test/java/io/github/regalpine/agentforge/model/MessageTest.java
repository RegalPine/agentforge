package io.github.regalpine.agentforge.model;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class MessageTest {

    @Test
    void shouldCreateSystemMessage() {
        SystemMessage msg = SystemMessage.of("You are a helpful assistant");

        assertEquals(MessageRole.SYSTEM, msg.role());
        assertEquals("You are a helpful assistant", msg.content());
    }

    @Test
    void shouldCreateUserMessage() {
        UserMessage msg = UserMessage.of("Hello, how are you?");

        assertEquals(MessageRole.USER, msg.role());
        assertEquals("Hello, how are you?", msg.content());
    }

    @Test
    void shouldCreateAssistantMessage() {
        AssistantMessage msg = AssistantMessage.of("I'm doing well, thank you!");

        assertEquals(MessageRole.ASSISTANT, msg.role());
        assertEquals("I'm doing well, thank you!", msg.content());
        assertTrue(msg.toolCalls().isEmpty());
    }

    @Test
    void shouldCreateAssistantMessageWithToolCalls() {
        ToolCall toolCall = ToolCall.of("call-1", "search", Map.of("query", "test"));
        AssistantMessage msg = AssistantMessage.of("", List.of(toolCall));

        assertEquals(MessageRole.ASSISTANT, msg.role());
        assertEquals(1, msg.toolCalls().size());
        assertEquals("call-1", msg.toolCalls().get(0).callId());
    }

    @Test
    void shouldCreateToolMessage() {
        ToolMessage msg = ToolMessage.success("call-1", "Search results: ...");

        assertEquals(MessageRole.TOOL, msg.role());
        assertEquals("call-1", msg.callId());
        assertEquals("Search results: ...", msg.content());
        assertEquals(ToolResultStatus.SUCCESS, msg.status());
    }

    @Test
    void shouldCreateFailedToolMessage() {
        ToolMessage msg = ToolMessage.failure("call-1", "Error: not found");

        assertEquals(ToolResultStatus.FAILED, msg.status());
    }

    @Test
    void shouldRejectNullContent() {
        assertThrows(NullPointerException.class, () -> new SystemMessage(null));
        assertThrows(NullPointerException.class, () -> new UserMessage(null));
    }

    @Test
    void shouldRejectBlankContent() {
        assertThrows(NullPointerException.class, () -> SystemMessage.of(null));
    }
}
