package io.github.regalpine.agentforge.model;

import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ToolCallTest {

    @Test
    void shouldCreateToolCall() {
        ToolCall call = ToolCall.of("call-1", "search", Map.of("query", "test"));

        assertEquals("call-1", call.callId());
        assertEquals("search", call.toolId());
        assertEquals("test", call.arguments().get("query"));
    }

    @Test
    void shouldCreateToolCallWithNoArguments() {
        ToolCall call = ToolCall.of("call-1", "search");

        assertEquals("call-1", call.callId());
        assertEquals("search", call.toolId());
        assertTrue(call.arguments().isEmpty());
    }

    @Test
    void shouldRejectNullCallId() {
        assertThrows(NullPointerException.class, () ->
            new ToolCall(null, "search", Map.of())
        );
    }

    @Test
    void shouldRejectNullToolId() {
        assertThrows(NullPointerException.class, () ->
            new ToolCall("call-1", null, Map.of())
        );
    }

    @Test
    void shouldRejectBlankCallId() {
        assertThrows(IllegalArgumentException.class, () ->
            ToolCall.of("", "search", Map.of())
        );
    }

    @Test
    void shouldRejectBlankToolId() {
        assertThrows(IllegalArgumentException.class, () ->
            ToolCall.of("call-1", "", Map.of())
        );
    }

    @Test
    void shouldCopyArgumentDefensively() {
        Map<String, Object> args = new HashMap<>();
        args.put("key", "value");
        ToolCall call = ToolCall.of("call-1", "search", args);

        // Map.copyOf creates a defensive copy from mutable maps
        assertNotSame(args, call.arguments());
        assertEquals("value", call.arguments().get("key"));

        // Mutating original should not affect the copy
        args.put("extra", "data");
        assertNull(call.arguments().get("extra"));
    }

    @Test
    void shouldAcceptImmutableMapArguments() {
        Map<String, Object> args = Map.of("key", "value");
        ToolCall call = ToolCall.of("call-1", "search", args);

        assertEquals("value", call.arguments().get("key"));
    }
}
