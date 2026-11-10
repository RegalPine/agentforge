package io.github.regalpine.agentforge.sdk.harness;

import io.github.regalpine.agentforge.model.*;
import io.github.regalpine.agentforge.sdk.InvocationResult;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.*;

class TestHarnessTest {

    @Test
    void fakeChatModelShouldReturnScriptedResponse() {
        FakeChatModel model = FakeChatModel.withResponse("Hello, world!");
        ChatResponse response = model.chat(ChatRequest.of(List.of(UserMessage.of("Hi"))));
        assertEquals("Hello, world!", response.content());
        assertEquals(1, model.callCount());
    }

    @Test
    void fakeChatModelShouldReturnMultipleResponses() {
        FakeChatModel model = FakeChatModel.builder()
            .response("First")
            .response("Second")
            .build();

        List<Message> msgs = List.of(UserMessage.of("test"));
        assertEquals("First", model.chat(ChatRequest.of(msgs)).content());
        assertEquals("Second", model.chat(ChatRequest.of(msgs)).content());
        // Last response repeats
        assertEquals("Second", model.chat(ChatRequest.of(msgs)).content());
        assertEquals(3, model.callCount());
    }

    @Test
    void fakeChatModelShouldTrackRequests() {
        FakeChatModel model = FakeChatModel.withResponse("ok");
        model.chat(ChatRequest.of(List.of(UserMessage.of("Q1"))));
        model.chat(ChatRequest.of(List.of(UserMessage.of("Q2"))));

        assertEquals(2, model.receivedRequests().size());
    }

    @Test
    void fakeChatModelShouldReset() {
        FakeChatModel model = FakeChatModel.withResponse("ok");
        List<Message> msgs = List.of(UserMessage.of("test"));
        model.chat(ChatRequest.of(msgs));
        model.chat(ChatRequest.of(msgs));
        model.reset();
        assertEquals(0, model.callCount());
        assertTrue(model.receivedRequests().isEmpty());
    }

    @Test
    void fakeCapabilityShouldReturnFixedResponse() {
        FakeCapability cap = FakeCapability.withResponse("search", Map.of("results", List.of("a", "b")));
        InvocationResult result = cap.invoke(Map.of("query", "test"));
        assertTrue(result.success());
        assertEquals(1, cap.invocationCount());
    }

    @Test
    void fakeCapabilityShouldUseHandler() {
        FakeCapability cap = FakeCapability.withHandler("calc",
            input -> {
                int a = (int) input.get("a");
                int b = (int) input.get("b");
                return InvocationResult.success(Map.of("sum", a + b));
            });

        InvocationResult result = cap.invoke(Map.of("a", 3, "b", 4));
        assertTrue(result.success());
        assertEquals(7, result.output().get("sum"));
    }

    @Test
    void fakeCapabilityShouldTrackInvocations() {
        FakeCapability cap = FakeCapability.withResponse("tool", Map.of());
        cap.invoke(Map.of());
        cap.invoke(Map.of());
        cap.invoke(Map.of());
        assertEquals(3, cap.invocationCount());
    }
}
