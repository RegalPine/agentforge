package io.github.regalpine.agentforge.multimodal;

/**
 * Realtime transport type.
 * AF-ENHANCE-13 §33: Transport.
 */
public enum RealtimeTransport {

    WEBSOCKET,

    SSE,

    HTTP_STREAMING,

    JAVA_FLOW
}
