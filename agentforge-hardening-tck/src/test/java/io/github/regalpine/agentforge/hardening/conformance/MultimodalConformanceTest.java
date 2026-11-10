package io.github.regalpine.agentforge.hardening.conformance;

import io.github.regalpine.agentforge.multimodal.*;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * conformance-multimodal:
 * ContentPart / MultimodalContent /
 * RealtimeSession.
 * AF-AD-16 §8.4, T-X12.13.
 */
class MultimodalConformanceTest {

    // ── ContentType ──

    @Test
    void afMm001_sevenContentTypes() {
        assertEquals(
            7, ContentType.values().length
        );
        assertNotNull(ContentType.TEXT);
        assertNotNull(ContentType.IMAGE);
        assertNotNull(ContentType.AUDIO);
        assertNotNull(ContentType.VIDEO);
        assertNotNull(ContentType.FILE);
        assertNotNull(ContentType.JSON);
        assertNotNull(ContentType.BINARY);
    }

    // ── TextContent ──

    @Test
    void afMm002_textContent() {
        var text = TextContent.of(
            "hello world"
        );
        assertEquals(
            ContentType.TEXT, text.type()
        );
        assertEquals(
            "hello world",
            text.value()
        );
    }

    // ── JsonContent ──

    @Test
    void afMm003_jsonContent() {
        var json = JsonContent.of(
            "{\"key\": \"value\"}"
        );
        assertEquals(
            ContentType.JSON, json.type()
        );
    }

    // ── ImageContent ──

    @Test
    void afMm004_imageContent() {
        var img = new ImageContent(
            "artifact://img-1",
            1024, 768, "png",
            "image/png", 2048L,
            Map.of()
        );
        assertEquals(
            ContentType.IMAGE, img.type()
        );
        assertEquals(1024,
            img.width());
        assertEquals(768,
            img.height());
    }

    // ── MultimodalContent ──

    @Test
    void afMm005_multimodalAggregation() {
        var mc = MultimodalContent.of(
            TextContent.of("hello"),
            new ImageContent(
                "artifact://img",
                100, 100, "png",
                "image/png", 100L,
                Map.of()
            )
        );
        assertEquals(2, mc.size());
        assertTrue(
            mc.containsType(
                ContentType.TEXT
            )
        );
        assertTrue(
            mc.containsType(
                ContentType.IMAGE
            )
        );
        assertFalse(
            mc.containsType(
                ContentType.AUDIO
            )
        );
    }

    @Test
    void afMm006_textFactory() {
        var mc = MultimodalContent.text(
            "simple text"
        );
        assertFalse(mc.isEmpty());
        assertEquals(1, mc.size());
    }

    @Test
    void afMm007_emptyContent() {
        var mc = new MultimodalContent(
            List.of(), Map.of()
        );
        assertTrue(mc.isEmpty());
        assertEquals(0, mc.size());
    }

    // ── RealtimeStatus ──

    @Test
    void afMm008_tenRealtimeStatuses() {
        assertEquals(
            10,
            RealtimeStatus.values().length
        );
        assertTrue(
            RealtimeStatus.CONNECTED
                .isConnected()
        );
        assertTrue(
            RealtimeStatus.CLOSED
                .isTerminal()
        );
        assertTrue(
            RealtimeStatus.FAILED
                .isTerminal()
        );
    }

    // ── RealtimeTransport ──

    @Test
    void afMm009_fourTransports() {
        assertEquals(
            4,
            RealtimeTransport.values().length
        );
        assertNotNull(
            RealtimeTransport.WEBSOCKET
        );
        assertNotNull(
            RealtimeTransport.SSE
        );
    }

    // ── RealtimeEvent ──

    @Test
    void afMm010_realtimeEvent() {
        var evt = RealtimeEvent.of(
            "message", 1L,
            Map.of("text", "hi")
        );
        assertNotNull(
            evt.eventId()
        );
        assertEquals("message",
            evt.type());
    }
}
