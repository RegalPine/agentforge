/**
 * Multimodal viewer models.
 * AF-ENHANCE-11 §3: ContentPart / MultimodalContent / Realtime.
 *
 * Maps to backend agentforge-multimodal types.
 */

// ── Content Types ──

/**
 * ContentTypeView - multimodal content type.
 */
export type ContentTypeView =
  | "TEXT"
  | "IMAGE"
  | "AUDIO"
  | "VIDEO"
  | "JSON"
  | "BINARY"
  | "FILE";

// ── Content Part ──

/**
 * ContentPartView - single content part in multimodal message.
 */
export interface ContentPartView {
  readonly partId: string;
  readonly type: ContentTypeView;
  readonly mimeType: string;
  readonly sizeBytes: number;
  readonly reference?: string;
  readonly textContent?: string;
  readonly metadata?: Record<string, string>;
}

// ── Multimodal Content ──

/**
 * MultimodalContentView - aggregated multimodal content.
 */
export interface MultimodalContentView {
  readonly contentId: string;
  readonly parts: ContentPartView[];
  readonly totalSizeBytes: number;
  readonly isEmpty: boolean;
}

// ── Image Content ──

/**
 * ImageContentView - image-specific content detail.
 */
export interface ImageContentView {
  readonly artifactReference: string;
  readonly width: number;
  readonly height: number;
  readonly format: string;
  readonly mimeType: string;
  readonly sizeBytes: number;
  readonly metadata: Record<string, string>;
}

// ── Realtime ──

/**
 * RealtimeSessionView - realtime session status.
 */
export interface RealtimeSessionView {
  readonly sessionId: string;
  readonly transport: RealtimeTransportView;
  readonly status: RealtimeStatusView;
  readonly eventCount: number;
  readonly connectedAt?: string;
  readonly disconnectedAt?: string;
}

export type RealtimeStatusView =
  | "CREATED"
  | "CONNECTING"
  | "CONNECTED"
  | "AUTHENTICATING"
  | "READY"
  | "STREAMING"
  | "PAUSED"
  | "DISCONNECTING"
  | "DISCONNECTED"
  | "FAILED";

export type RealtimeTransportView =
  | "SSE"
  | "WEBSOCKET"
  | "LONG_POLL"
  | "GRPC_STREAM";

// ── Realtime Event ──

/**
 * RealtimeEventView - event in a realtime stream.
 */
export interface RealtimeEventView {
  readonly type: string;
  readonly sequence: number;
  readonly payload: Record<string, unknown>;
  readonly timestamp: string;
}
