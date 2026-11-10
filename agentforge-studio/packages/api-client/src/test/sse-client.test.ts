import { afterEach, describe, expect, it, vi } from "vitest";
import { ExecutionEventSource } from "../sse-client.js";

const config = { baseUrl: "http://localhost", tenantId: "tenant-a", authToken: "session-a" };

afterEach(() => { vi.unstubAllGlobals(); vi.useRealTimers(); });

describe("SSE 身份与连接生命周期", () => {
  it("使用请求头认证并处理跨分块的事件", async () => {
    let stream!: ReadableStreamDefaultController<Uint8Array>;
    vi.stubGlobal("fetch", vi.fn().mockResolvedValue(new Response(new ReadableStream({
      start(controller) { stream = controller; },
    }), { headers: { "Content-Type": "text/event-stream" } })));
    const source = new ExecutionEventSource(config, "exec-1");
    const listener = vi.fn();
    source.on("*", listener);
    source.connect();
    await vi.waitFor(() => expect(fetch).toHaveBeenCalledOnce());
    const [url, init] = vi.mocked(fetch).mock.calls[0];
    expect(url).not.toContain("session-a");
    expect(init?.headers).toMatchObject({ Authorization: "Bearer session-a", "X-Tenant-Id": "tenant-a" });
    stream.enqueue(new TextEncoder().encode("event: update\r\ndata: {\"type\":\"DONE\","));
    stream.enqueue(new TextEncoder().encode("\"sequence\":1}\r\n\r\n"));
    await vi.waitFor(() => expect(listener).toHaveBeenCalledWith({ type: "DONE", sequence: 1 }));
    source.disconnect();
    expect((init?.signal as AbortSignal).aborted).toBe(true);
  });

  it("认证拒绝后不重连，也不假装连接成功", async () => {
    vi.stubGlobal("fetch", vi.fn().mockResolvedValue(new Response("denied", { status: 401 })));
    const source = new ExecutionEventSource(config, "exec-1");
    const errors = vi.fn();
    source.onError(errors);
    source.connect();
    await vi.waitFor(() => expect(errors).toHaveBeenCalledOnce());
    expect(fetch).toHaveBeenCalledOnce();
    source.disconnect();
  });

  it("身份切换后的旧流不会向新页面派发事件", async () => {
    let stream!: ReadableStreamDefaultController<Uint8Array>;
    let token = "a";
    vi.stubGlobal("fetch", vi.fn().mockResolvedValue(new Response(new ReadableStream({
      start(controller) { stream = controller; },
    }), { headers: { "Content-Type": "text/event-stream" } })));
    const source = new ExecutionEventSource({ ...config, getAuth: () => ({ authToken: token, tenantId: "tenant-a" }) }, "exec-1");
    const listener = vi.fn();
    source.on("*", listener);
    source.connect();
    await vi.waitFor(() => expect(fetch).toHaveBeenCalledOnce());
    token = "b";
    stream.enqueue(new TextEncoder().encode('data: {"type":"DONE","sequence":1}\n\n'));
    await new Promise(resolve => setTimeout(resolve, 20));
    expect(listener).not.toHaveBeenCalled();
    source.disconnect();
  });
});
