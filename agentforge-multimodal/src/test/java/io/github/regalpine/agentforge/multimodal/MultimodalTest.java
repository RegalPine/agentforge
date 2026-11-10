package io.github.regalpine.agentforge.multimodal;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for Multimodal types.
 * AF-ENHANCE-13.
 */
class MultimodalTest {

    // --- ContentType ---

    @Test
    void contentType_values() {
        assertEquals(
            7, ContentType.values().length
        );
    }

    // --- TextContent ---

    @Test
    void textContent_basic() {
        var t = TextContent.of("hello");

        assertEquals(ContentType.TEXT, t.type());
        assertEquals("hello", t.text());
        assertEquals("hello", t.value());
        assertTrue(t.metadata().isEmpty());
    }

    @Test
    void textContent_rejectsNull() {
        assertThrows(
            NullPointerException.class,
            () -> TextContent.of(null)
        );
    }

    // --- ImageContent ---

    @Test
    void imageContent_basic() {
        var img = new ImageContent(
            "artifact://img/1",
            800, 600, "png",
            "image/png", 1024,
            Map.of()
        );

        assertEquals(
            ContentType.IMAGE, img.type()
        );
        assertEquals(800, img.width());
        assertEquals(600, img.height());
        assertEquals(
            "artifact://img/1",
            img.value()
        );
    }

    // --- AudioContent ---

    @Test
    void audioContent_basic() {
        var audio = new AudioContent(
            "artifact://audio/1",
            "pcm", 44100, 2,
            5000, "audio/wav",
            Map.of()
        );

        assertEquals(
            ContentType.AUDIO, audio.type()
        );
        assertEquals(44100, audio.sampleRate());
        assertEquals(2, audio.channels());
    }

    // --- VideoContent ---

    @Test
    void videoContent_basic() {
        var video = new VideoContent(
            "artifact://video/1",
            "h264", 1920, 1080,
            30.0, 60000,
            "video/mp4", Map.of()
        );

        assertEquals(
            ContentType.VIDEO, video.type()
        );
        assertEquals(1920, video.width());
    }

    // --- FileContent ---

    @Test
    void fileContent_basic() {
        var file = new FileContent(
            "artifact://file/1",
            "doc.pdf",
            "application/pdf",
            2048, Map.of()
        );

        assertEquals(
            ContentType.FILE, file.type()
        );
        assertEquals("doc.pdf", file.fileName());
    }

    // --- JsonContent ---

    @Test
    void jsonContent_basic() {
        var json = JsonContent.of(
            Map.of("key", "value")
        );

        assertEquals(
            ContentType.JSON, json.type()
        );
        assertNotNull(json.value());
    }

    // --- MultimodalContent ---

    @Test
    void multimodal_text() {
        var mc = MultimodalContent.text(
            "hello"
        );

        assertFalse(mc.isEmpty());
        assertEquals(1, mc.size());
        assertTrue(
            mc.containsType(ContentType.TEXT)
        );
    }

    @Test
    void multimodal_mixed() {
        var mc = MultimodalContent.of(
            TextContent.of("analyze"),
            new ImageContent(
                "artifact://img/1",
                800, 600, "png",
                "image/png", 1024,
                Map.of()
            )
        );

        assertEquals(2, mc.size());
        assertTrue(
            mc.containsType(ContentType.TEXT)
        );
        assertTrue(
            mc.containsType(ContentType.IMAGE)
        );
        assertFalse(
            mc.containsType(ContentType.AUDIO)
        );
    }

    @Test
    void multimodal_empty() {
        var mc = new MultimodalContent(
            java.util.List.of(), Map.of()
        );

        assertTrue(mc.isEmpty());
        assertEquals(0, mc.size());
    }

    // --- RealtimeStatus ---

    @Test
    void realtimeStatus_connected() {
        assertTrue(
            RealtimeStatus.CONNECTED.isConnected()
        );
        assertTrue(
            RealtimeStatus.ACTIVE.isConnected()
        );
        assertFalse(
            RealtimeStatus.CREATED.isConnected()
        );
    }

    @Test
    void realtimeStatus_terminal() {
        assertTrue(
            RealtimeStatus.CLOSED.isTerminal()
        );
        assertTrue(
            RealtimeStatus.FAILED.isTerminal()
        );
        assertFalse(
            RealtimeStatus.ACTIVE.isTerminal()
        );
    }

    // --- RealtimeEvent ---

    @Test
    void realtimeEvent_basic() {
        var evt = RealtimeEvent.of(
            "response.delta", 1L, "Hi"
        );

        assertEquals(
            "response.delta", evt.type()
        );
        assertEquals(1L, evt.sequence());
    }

    // --- InMemoryRealtimeSession ---

    @Test
    void realtimeSession_lifecycle() {
        var session =
            new InMemoryRealtimeSession(
                "rs-1",
                RealtimeTransport.WEBSOCKET
            );

        assertEquals(
            RealtimeStatus.CREATED,
            session.status()
        );

        session.connect().toCompletableFuture()
            .join();

        assertEquals(
            RealtimeStatus.CONNECTED,
            session.status()
        );

        var evt = RealtimeEvent.of(
            "input.delta", 1L, "hello"
        );

        session.send(evt).toCompletableFuture()
            .join();

        assertEquals(
            1, session.events().size()
        );

        session.close().toCompletableFuture()
            .join();

        assertEquals(
            RealtimeStatus.CLOSED,
            session.status()
        );
    }

    @Test
    void realtimeSession_rejectsNull() {
        assertThrows(
            IllegalArgumentException.class,
            () -> new InMemoryRealtimeSession(
                null,
                RealtimeTransport.WEBSOCKET
            )
        );
    }

    // --- RealtimeTransport ---

    @Test
    void transport_values() {
        assertEquals(
            4,
            RealtimeTransport.values().length
        );
    }
}
