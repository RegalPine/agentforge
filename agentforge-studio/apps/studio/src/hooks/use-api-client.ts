import { useMemo } from "react";
import { AgentForgeApiClient, HttpClient, type ApiClientConfig } from "@agentforge/api-client";
import { useAuthStore } from "../stores/auth-store";

/**
 * Shared API client factory.
 *
 * Creates a singleton-like client based on Vite env.
 * Pages consume this via query hooks, never create their own client.
 *
 * Auth integration: the client's token and tenantId are synced
 * from the auth store on every render, so login/logout automatically
 * updates all subsequent API requests.
 */
let _client: AgentForgeApiClient | null = null;
let _http: HttpClient | null = null;

export function getApiClientConfig(): ApiClientConfig {
  return {
    baseUrl: (import.meta as unknown as Record<string, Record<string, string>>).env?.VITE_API_BASE_URL ?? "",
    tenantId: "default",
    getAuth: () => {
      const { token, user } = useAuthStore.getState();
      return { authToken: token ?? undefined, tenantId: user?.tenantId ?? "default" };
    },
  };
}

function getHttp(): HttpClient {
  if (!_http) _http = new HttpClient(getApiClientConfig());
  return _http;
}

export function getApiClient(): AgentForgeApiClient {
  if (!_client) {
    _client = new AgentForgeApiClient(getHttp());
  }
  return _client;
}

/**
 * Sync auth state (token + tenantId) into the shared HTTP client.
 *
 * Call this from a component that has access to the auth store.
 * Typically used once at the app root level.
 */
export function syncAuthState(token: string | null, tenantId: string | null): void {
  const http = getHttp();
  if (token) {
    http.updateAuthToken(token);
  } else {
    http.updateAuthToken("");
  }
  http.updateTenantId(tenantId ?? "default");
}

/**
 * Hook to access the shared API client.
 *
 * Automatically syncs auth state from the auth store so that
 * all API requests carry the current Bearer token and tenant header.
 */
export function useApiClient(): AgentForgeApiClient {
  const token = useAuthStore((s) => s.token);
  const user = useAuthStore((s) => s.user);

  return useMemo(() => new AgentForgeApiClient(getHttp()), [token, user?.tenantId]);
}
