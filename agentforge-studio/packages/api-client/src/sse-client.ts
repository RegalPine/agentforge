import type { ExecutionEvent } from "@agentforge/model";
import { ApiError, type ApiClientConfig } from "./config.js";

/**
 * SSE Client for real-time execution event streaming.
 *
 * Supports replay via lastSequence to handle disconnections.
 * Event source is the backend EventStore, not browser state.
 */
export class ExecutionEventSource {
  private controller: AbortController | null = null;
  private stopped = true;
  private readonly openListeners: Array<() => void> = [];
  private lastSequence: number = 0;
  private reconnectTimer: ReturnType<typeof setTimeout> | null = null;
  private readonly listeners: Map<
    string,
    Array<(event: ExecutionEvent) => void>
  > = new Map();
  private readonly errorListeners: Array<(error: Error) => void> = [];

  constructor(
    private readonly config: ApiClientConfig,
    private readonly executionId: string,
  ) {}

  /**
   * Start listening for execution events.
   * If after is provided, resumes from that sequence (replay).
   */
  connect(after?: number): void {
    if (after != null) this.lastSequence = after;
    if (this.reconnectTimer) clearTimeout(this.reconnectTimer);
    this.reconnectTimer = null;
    this.controller?.abort();
    this.stopped = false;
    const controller = new AbortController();
    this.controller = controller;
    void this.consume(controller);
  }

  onOpen(handler: () => void): () => void {
    this.openListeners.push(handler);
    return () => {
      const index = this.openListeners.indexOf(handler);
      if (index >= 0) this.openListeners.splice(index, 1);
    };
  }

  private async consume(controller: AbortController): Promise<void> {
    const auth = this.config.getAuth?.() ?? this.config;
    const current = () => {
      const now = this.config.getAuth?.() ?? this.config;
      return !controller.signal.aborted && now.authToken === auth.authToken && now.tenantId === auth.tenantId;
    };
    const query = this.lastSequence > 0 ? `?after=${this.lastSequence}` : "";
    const url = `${this.config.baseUrl.replace(/\/$/, "")}/api/v1/executions/${encodeURIComponent(this.executionId)}/events/stream${query}`;
    const headers: Record<string, string> = { Accept: "text/event-stream", "X-Tenant-Id": auth.tenantId };
    if (auth.authToken) headers.Authorization = `Bearer ${auth.authToken}`;
    let reader: ReadableStreamDefaultReader<Uint8Array> | undefined;
    try {
      const response = await fetch(url, { headers, signal: controller.signal });
      if (!response.ok) throw new ApiError(response.status, "SSE_REJECTED", "事件流连接被拒绝");
      if (!response.body || !response.headers.get("Content-Type")?.includes("text/event-stream")) {
        throw new ApiError(415, "SSE_INVALID_RESPONSE", "响应不是事件流");
      }
      reader = response.body.getReader();
      if (!current()) return;
      for (const listener of this.openListeners) listener();
      const decoder = new TextDecoder();
      let buffer = "";
      while (current()) {
        const { value, done } = await reader.read();
        if (!current()) return;
        if (done) break;
        buffer += decoder.decode(value, { stream: true });
        if (buffer.length > 1_048_576) throw new ApiError(413, "SSE_TOO_LARGE", "事件过大");
        let separator: RegExpExecArray | null;
        while ((separator = /\x0d\x0a\x0d\x0a|\x0a\x0a|\x0d\x0d/.exec(buffer))) {
          const block = buffer.slice(0, separator.index);
          buffer = buffer.slice(separator.index + separator[0].length);
          const data = block.split(/\r\n|\r|\n/).filter(line => line.startsWith("data:"))
            .map(line => line.slice(5).replace(/^ /, "")).join("\n");
          if (!data) continue;
          const event = JSON.parse(data) as ExecutionEvent;
          if (!Number.isSafeInteger(event.sequence) || typeof event.type !== "string") {
            throw new ApiError(422, "SSE_INVALID_EVENT", "事件格式不正确");
          }
          if (event.sequence <= this.lastSequence) continue;
          this.lastSequence = event.sequence;
          for (const listener of this.listeners.get(event.type) ?? []) listener(event);
          for (const listener of this.listeners.get("*") ?? []) listener(event);
        }
      }
      if (current()) throw new Error("事件流已关闭");
    } catch (error) {
      if (!current()) return;
      for (const listener of this.errorListeners) listener(error instanceof Error ? error : new Error("事件流错误"));
      if (!(error instanceof ApiError && error.isClientError)) this.scheduleReconnect();
    } finally {
      await reader?.cancel().catch(() => {});
      reader?.releaseLock();
    }
  }

  /**
   * Subscribe to a specific event type, or "*" for all events.
   */
  on(
    eventType: string,
    handler: (event: ExecutionEvent) => void,
  ): () => void {
    if (!this.listeners.has(eventType)) {
      this.listeners.set(eventType, []);
    }
    this.listeners.get(eventType)!.push(handler);

    return () => {
      const handlers = this.listeners.get(eventType);
      if (handlers) {
        const idx = handlers.indexOf(handler);
        if (idx >= 0) handlers.splice(idx, 1);
      }
    };
  }

  onError(handler: (error: Error) => void): () => void {
    this.errorListeners.push(handler);
    return () => {
      const idx = this.errorListeners.indexOf(handler);
      if (idx >= 0) this.errorListeners.splice(idx, 1);
    };
  }

  /**
   * Get the last received sequence number (for reconnection).
   */
  getLastSequence(): number {
    return this.lastSequence;
  }

  /**
   * Disconnect and clean up.
   */
  disconnect(): void {
    if (this.reconnectTimer) {
      clearTimeout(this.reconnectTimer);
      this.reconnectTimer = null;
    }
    this.stopped = true;
    this.controller?.abort();
    this.controller = null;
    this.openListeners.length = 0;
    this.listeners.clear();
    this.errorListeners.length = 0;
  }

  private scheduleReconnect(): void {
    if (this.stopped || this.reconnectTimer) return;

    this.reconnectTimer = setTimeout(() => {
      this.reconnectTimer = null;
      this.connect(this.lastSequence);
    }, 3000);
  }
}
