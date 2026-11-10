import { useEffect, useMemo, type ReactNode } from "react";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { Navigate, useLocation } from "react-router-dom";
import { useAuthStore, selectIsAuthenticated } from "../stores/auth-store.js";

interface AuthGuardProps {
  children: ReactNode;
}

/**
 * AuthGuard - protects routes that require authentication.
 *
 * If the user is not authenticated, redirects to /login
 * with the original path preserved as a query parameter.
 */
export function AuthGuard({ children }: AuthGuardProps) {
  const location = useLocation();
  const isAuthenticated = useAuthStore(selectIsAuthenticated);
  const restoring = useAuthStore((s) => s.restoring);
  const token = useAuthStore((s) => s.token);
  const tenantId = useAuthStore((s) => s.user?.tenantId);
  const userId = useAuthStore((s) => s.user?.userId);
  // 查询缓存和页面局部状态均按身份隔离，切换后不显示上一租户的数据。
  const scope = useMemo(() => ({
    key: crypto.randomUUID(),
    client: new QueryClient({ defaultOptions: { queries: { staleTime: 30_000, retry: 1 } } }),
  }), [token, tenantId, userId]);
  useEffect(() => () => scope.client.clear(), [scope]);

  // Wait for session restoration to complete
  if (restoring) {
    return null;
  }

  if (!isAuthenticated) {
    return (
      <Navigate
        to={`/login?redirect=${encodeURIComponent(location.pathname)}`}
        replace
      />
    );
  }

  return <QueryClientProvider key={scope.key} client={scope.client}>{children}</QueryClientProvider>;
}
