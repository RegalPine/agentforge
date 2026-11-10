import { act, renderHook, render, screen, cleanup } from "@testing-library/react";
import { MemoryRouter } from "react-router-dom";
import { useQueryClient } from "@tanstack/react-query";
import { useState } from "react";
import { AuthGuard } from "../components/AuthGuard";
import { readFileSync, readdirSync } from "node:fs";
import { resolve } from "node:path";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import { getApiClient, useApiClient } from "../hooks/use-api-client";
import { useAuthStore, type AuthUser } from "../stores/auth-store";

const user = (tenantId: string): AuthUser => ({
  userId: tenantId, username: tenantId, displayName: tenantId,
  tenantId, roles: [], permissions: [],
});

describe("业务请求身份传播", () => {
  beforeEach(() => {
    useAuthStore.setState({ token: null, user: null, restoring: false });
    vi.stubGlobal("fetch", vi.fn().mockResolvedValue({ ok: true, status: 200, json: async () => [] }));
  });
  afterEach(() => { cleanup(); vi.unstubAllGlobals(); });

  it("非组件调用也在请求时读取当前身份，退出不沿用旧 token", async () => {
    const api = getApiClient();
    useAuthStore.setState({ token: "session-a", user: user("tenant-a") });
    await api.listGraphs();
    expect(vi.mocked(fetch).mock.calls.at(-1)?.[1]?.headers).toMatchObject({
      Authorization: "Bearer session-a", "X-Tenant-Id": "tenant-a",
    });
    useAuthStore.getState().logout();
    await api.listGraphs();
    const headers = vi.mocked(fetch).mock.calls.at(-1)?.[1]?.headers;
    expect(headers).not.toHaveProperty("Authorization");
    expect(headers).not.toHaveProperty("X-Tenant-Id", "tenant-a");
  });

  it("所有页面禁止绕过共享 client 发业务请求", () => {
    const pages = resolve(process.cwd(), "src/pages");
    for (const file of readdirSync(pages).filter(name => name.endsWith(".tsx"))) {
      const source = readFileSync(resolve(pages, file), "utf8");
      expect(source, file).not.toMatch(/\bfetch\s*\(/);
      expect(source, file).not.toMatch(/new\s+(HttpClient|AgentForgeApiClient)\s*\(/);
    }
  });

  it("身份切换重建页面状态与查询缓存", () => {
    useAuthStore.setState({ token: "session-a", user: user("tenant-a") });
    const clients: ReturnType<typeof useQueryClient>[] = [];
    function Probe() {
      const queryClient = useQueryClient();
      clients.push(queryClient);
      const [tenant] = useState(() => useAuthStore.getState().user?.tenantId);
      return <span>{tenant}</span>;
    }
    render(<MemoryRouter><AuthGuard><Probe /></AuthGuard></MemoryRouter>);
    const old = clients.at(-1)!;
    old.setQueryData(["graphs"], ["private-a"]);
    act(() => useAuthStore.setState({ token: "session-b", user: user("tenant-b") }));
    expect(screen.queryByText("tenant-a")).toBeNull();
    expect(screen.getByText("tenant-b")).toBeTruthy();
    expect(clients.at(-1)).not.toBe(old);
    expect(clients.at(-1)!.getQueryData(["graphs"])).toBeUndefined();
  });

  it("身份切换使 hook 引用更新，已持有的 client 也不能发送旧身份", async () => {
    useAuthStore.setState({ token: "session-a", user: user("tenant-a") });
    const { result, unmount } = renderHook(() => useApiClient());
    const previous = result.current;
    act(() => useAuthStore.setState({ token: "session-b", user: user("tenant-b") }));
    expect(result.current).not.toBe(previous);
    await previous.listGraphs();
    expect(vi.mocked(fetch).mock.calls.at(-1)?.[1]?.headers).toMatchObject({
      Authorization: "Bearer session-b", "X-Tenant-Id": "tenant-b",
    });
    unmount();
  });
});
