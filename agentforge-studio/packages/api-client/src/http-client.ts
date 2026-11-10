import type { ApiClientConfig } from "./config.js";
import { ApiError } from "./config.js";

/**
 * HttpClient - thin wrapper around fetch with tenant context and auth.
 */
export class HttpClient {
  private readonly baseUrl: string;
  private readonly config: ApiClientConfig;

  constructor(config: ApiClientConfig) {
    this.config = config;
    this.baseUrl = config.baseUrl.replace(/\/$/, "");
  }

  async get<T>(path: string): Promise<T> {
    return this.request<T>("GET", path);
  }

  async post<T>(path: string, body: unknown): Promise<T> {
    return this.request<T>("POST", path, body);
  }

  async put<T>(path: string, body: unknown): Promise<T> {
    return this.request<T>("PUT", path, body);
  }

  async delete(path: string): Promise<void> {
    await this.request<void>("DELETE", path);
  }

  async request<T>(
    method: string,
    path: string,
    body?: unknown,
  ): Promise<T> {
    const url = `${this.baseUrl}${path}`;
    const auth = this.config.getAuth?.() ?? this.config;
    const headers: Record<string, string> = {
      "Content-Type": "application/json",
      "Accept": "application/json",
      "X-Tenant-Id": auth.tenantId,
    };

    if (auth.authToken) {
      headers["Authorization"] = `Bearer ${auth.authToken}`;
    }

    const response = await fetch(url, {
      method,
      headers,
      body: body != null ? JSON.stringify(body) : undefined,
      signal: this.config.timeoutMs
        ? AbortSignal.timeout(this.config.timeoutMs)
        : undefined,
    });

    const latestAuth = this.config.getAuth?.();
    if (latestAuth && (latestAuth.authToken !== auth.authToken || latestAuth.tenantId !== auth.tenantId)) {
      throw new ApiError(401, "AUTH_CONTEXT_CHANGED", "身份已切换，丢弃旧请求结果");
    }

    if (!response.ok) {
      const errorBody = await response.json().catch(() => ({}));
      throw new ApiError(
        response.status,
        (errorBody as Record<string, string>).code ?? "UNKNOWN",
        (errorBody as Record<string, string>).message ?? response.statusText,
      );
    }

    if (response.status === 204) {
      return undefined as T;
    }

    const result = await response.json() as T;
    const completedAuth = this.config.getAuth?.();
    if (completedAuth && (completedAuth.authToken !== auth.authToken || completedAuth.tenantId !== auth.tenantId)) {
      throw new ApiError(401, "AUTH_CONTEXT_CHANGED", "身份已切换，丢弃旧请求结果");
    }
    return result;
  }

  updateTenantId(tenantId: string): void {
    this.config.tenantId = tenantId;
  }

  updateAuthToken(authToken: string): void {
    this.config.authToken = authToken;
  }
}
